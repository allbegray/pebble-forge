package io.github.allbegray.pebbleforge.contracts;

import io.github.allbegray.pebbleforge.component.Components;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.LoaderException;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class CheckSession {

    private final PebbleEngine engine;
    private final Components components;
    private final ModelSchema model;
    private final Set<String> rootTemplates;
    private final Set<String> partialTemplates;

    private final Map<String, TemplateScan> scans = new TreeMap<>();
    private final Set<String> componentTargets = new HashSet<>();
    private final Map<String, Integer> contextCounts = new HashMap<>();
    private Map<String, Set<String>> relevantNames = Map.of();
    private static final int MAX_CONTEXTS_PER_TEMPLATE = 4096;
    private final Set<String> analyzed = new HashSet<>();
    private final Set<ContextKey> evaluated = new HashSet<>();
    private final Set<String> evaluatedTemplates = new HashSet<>();
    private final Deque<String> includeStack = new ArrayDeque<>();
    private final Set<String> reportedCycles = new HashSet<>();
    private final LinkedHashSet<ContractDiagnostic> diagnostics = new LinkedHashSet<>();

    CheckSession(PebbleEngine engine, Components components, ModelSchema model,
            Set<String> rootTemplates, Set<String> partialTemplates) {
        this.engine = engine;
        this.components = components;
        this.model = model;
        this.rootTemplates = rootTemplates;
        this.partialTemplates = partialTemplates;
    }

    ContractResult check(String templateName) {
        TemplateScan scan = scan(templateName);
        discover(List.of(templateName));
        computeRelevantNames();
        analyze(scan, null);
        evaluate(scan, Scope.EMPTY, null);
        return result();
    }

    ContractResult checkAll(Collection<String> templateNames) {
        SortedSet<String> universe = new TreeSet<>();
        for (String name : templateNames) {
            universe.add(Objects.requireNonNull(name, "templateName"));
        }

        SortedSet<String> seeds = new TreeSet<>(universe);
        seeds.addAll(rootTemplates);
        discover(seeds);
        computeRelevantNames();

        SortedSet<String> roots = inferRoots(universe);
        Set<String> reachable = reachableFrom(roots);
        for (String name : universe) {
            if (!roots.contains(name) && !isComponent(name)
                    && !isNestedComponent(name)
                    && !partialTemplates.contains(name) && !reachable.contains(name)) {
                roots.add(name);
            }
        }
        for (String root : roots) {
            analyze(scan(root), null);
        }
        for (String name : universe) {
            analyze(scan(name), null);
        }
        for (String root : roots) {
            evaluate(scan(root), Scope.EMPTY, null);
        }
        detectUnreachedCycles(universe);
        return result();
    }

    private void discover(Collection<String> seeds) {
        Deque<String> pending = new ArrayDeque<>(seeds);
        Set<String> visited = new HashSet<>();
        while (!pending.isEmpty()) {
            String name = pending.removeFirst();
            if (!visited.add(name)) {
                continue;
            }
            TemplateScan scan = scan(name);
            if (scan.failure != null) {
                continue;
            }
            forEachStaticTarget(scan, pending::addLast);
        }
    }

    private void computeRelevantNames() {
        Map<String, Set<String>> relevant = new HashMap<>();
        for (TemplateScan scan : scans.values()) {
            Set<String> names = new HashSet<>();
            for (TemplateScan.VariableRead read : scan.reads) {
                names.add(read.name());
            }
            relevant.put(scan.templateName, names);
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (TemplateScan scan : scans.values()) {
                Set<String> names = relevant.get(scan.templateName);
                if (names == null) {
                    continue;
                }
                for (TemplateScan.IncludeEdge edge : scan.includes) {
                    if (edge.resolutionError() == null
                            && edge.target() instanceof TemplateScan.StaticTarget target) {
                        Set<String> child = relevant.get(target.name());
                        if (child != null && names.addAll(child)) {
                            changed = true;
                        }
                    }
                }
                String parent = staticParent(scan);
                if (parent != null) {
                    Set<String> ancestor = relevant.get(parent);
                    if (ancestor != null && names.addAll(ancestor)) {
                        changed = true;
                    }
                }
            }
        }
        relevantNames = relevant;
    }

    private Set<String> reachableFrom(Collection<String> roots) {
        Set<String> reachable = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            String name = pending.removeFirst();
            if (!reachable.add(name)) {
                continue;
            }
            TemplateScan scan = scan(name);
            if (scan.failure != null) {
                continue;
            }
            forEachStaticTarget(scan, pending::addLast);
        }
        return reachable;
    }

    private static String staticParent(TemplateScan scan) {
        TemplateScan.ParentCall parent = scan.parent;
        if (parent == null || parent.dynamicTarget() || parent.resolutionError() != null) {
            return null;
        }
        return parent.resolvedTarget();
    }

    private void forEachStaticTarget(TemplateScan scan, Consumer<String> target) {
        String parent = staticParent(scan);
        if (parent != null) {
            target.accept(parent);
        }
        for (TemplateScan.IncludeEdge edge : scan.includes) {
            if (edge.resolutionError() == null
                    && edge.target() instanceof TemplateScan.StaticTarget include) {
                target.accept(include.name());
            }
        }
        for (TemplateScan.ComponentCall call : scan.componentCalls) {
            if (call.dynamicName()) {
                continue;
            }
            try {
                String path = components.resolvePath(call.name());
                componentTargets.add(path);
                target.accept(path);
            } catch (IllegalArgumentException error) {
                // Invalid names are reported at the call site during analysis.
            }
        }
    }

    private SortedSet<String> inferRoots(SortedSet<String> universe) {
        Set<String> incoming = new HashSet<>();
        for (TemplateScan scan : scans.values()) {
            String parent = staticParent(scan);
            if (parent != null) {
                incoming.add(parent);
            }
            for (TemplateScan.IncludeEdge edge : scan.includes) {
                if (edge.resolutionError() == null
                        && edge.target() instanceof TemplateScan.StaticTarget target) {
                    incoming.add(target.name());
                }
            }
        }

        SortedSet<String> roots = new TreeSet<>();
        for (String name : universe) {
            if (rootTemplates.contains(name)) {
                roots.add(name);
            } else if (isComponent(name)) {
                roots.add(name);
            } else if (partialTemplates.contains(name)) {
                continue;
            } else if (isNestedComponent(name)) {
                continue;
            } else if (!incoming.contains(name)) {
                roots.add(name);
            }
        }
        roots.addAll(rootTemplates);
        return roots;
    }

    private boolean isComponent(String name) {
        String root = components.root();
        if (root.isEmpty() || !name.startsWith(root + "/")) {
            return false;
        }
        String relative = name.substring(root.length() + 1);
        return !relative.contains("/") || componentTargets.contains(name);
    }

    private boolean isNestedComponent(String name) {
        String root = components.root();
        if (root.isEmpty() || !name.startsWith(root + "/")) {
            return false;
        }
        return name.substring(root.length() + 1).contains("/");
    }

    private TemplateScan scan(String name) {
        TemplateScan existing = scans.get(name);
        if (existing != null) {
            return existing;
        }

        TemplateScan scan = new TemplateScan(name);
        scans.put(name, scan);
        try {
            PebbleTemplate template = engine.getTemplate(name);
            if (!(template instanceof PebbleTemplateImpl implementation)) {
                scan.failure = new TemplateScan.LoadFailure(
                        "Template is not a PebbleTemplateImpl");
                return scan;
            }
            implementation.getRootNode().accept(new ContractVisitor(scan, implementation));
        } catch (LoaderException error) {
            scan.failure = new TemplateScan.LoadFailure(PebbleErrors.messageOf(error));
        } catch (PebbleException error) {
            scan.failure = new TemplateScan.ParseFailure(PebbleErrors.messageOf(error),
                    PebbleErrors.lineOf(error));
        }
        return scan;
    }

    private void analyze(TemplateScan scan, Site site) {
        if (scan.failure instanceof TemplateScan.LoadFailure) {
            reportFailure(scan, site);
            return;
        }
        if (scan.failure instanceof TemplateScan.ParseFailure) {
            if (site == null && !analyzed.add(scan.templateName)) {
                return;
            }
            reportFailure(scan, site);
            return;
        }
        if (!analyzed.add(scan.templateName)) {
            return;
        }
        reportInvalidParent(scan);
        for (TemplateScan.IncludeEdge edge : scan.includes) {
            analyzeInclude(scan, edge, site);
        }
        for (TemplateScan.ComponentCall call : scan.componentCalls) {
            analyzeComponentCall(scan, call);
        }
        analyzeParent(scan, site);
    }

    private void analyzeParent(TemplateScan scan, Site site) {
        TemplateScan.ParentCall parent = scan.parent;
        if (parent == null) {
            return;
        }
        if (parent.dynamicTarget()) {
            warning(scan.templateName, parent.line(),
                    "Extends target is dynamic and cannot be checked");
            return;
        }
        if (parent.resolutionError() != null) {
            return;
        }
        String target = parent.resolvedTarget();
        if (target == null) {
            return;
        }
        ExtendsSite parentSite = new ExtendsSite(scan.templateName, parent.line(),
                append(site, scan.templateName, parent.line()));
        analyze(scan(target), parentSite);
    }

    private void reportInvalidParent(TemplateScan scan) {
        TemplateScan.ParentCall parent = scan.parent;
        if (parent == null || parent.resolutionError() == null) {
            return;
        }
        error(scan.templateName, parent.line(), "Extends target ["
                + parent.literalTarget() + "] is invalid: " + parent.resolutionError());
    }

    private void reportFailure(TemplateScan scan, Site site) {
        if (scan.failure instanceof TemplateScan.LoadFailure load) {
            if (site == null) {
                error(scan.templateName, 0, "Could not load template: " + load.message());
            } else {
                error(site.callerTemplate(), site.line(), site.relation() + " template ["
                        + scan.templateName + "] was not found: " + load.message()
                        + chainSuffix(site.chain(), site.chainLabel()));
            }
            return;
        }
        TemplateScan.ParseFailure parse = (TemplateScan.ParseFailure) scan.failure;
        if (site == null) {
            error(scan.templateName, parse.line(),
                    "Could not parse template: " + parse.message());
        } else {
            error(scan.templateName, parse.line(), site.relation() + " template ["
                    + scan.templateName + "] could not be parsed: " + parse.message()
                    + chainSuffix(site.chain(), site.chainLabel()));
        }
    }

    private void analyzeInclude(TemplateScan scan, TemplateScan.IncludeEdge edge, Site site) {
        if (edge.target() instanceof TemplateScan.DynamicTarget) {
            warning(scan.templateName, edge.line(),
                    "Include target is dynamic and cannot be checked");
        }
        if (edge.dynamicWithKeys()) {
            warning(scan.templateName, edge.line(), "Include [" + targetLabel(edge)
                    + "] uses a dynamic with key; variables in the included template "
                    + "cannot be fully checked");
        } else if (edge.scopeIncomplete()) {
            warning(scan.templateName, edge.line(), "Include [" + targetLabel(edge)
                    + "] is inside a dynamic with scope; variables in the included "
                    + "template cannot be fully checked");
        }
        if (edge.target() instanceof TemplateScan.DynamicTarget) {
            return;
        }
        if (edge.resolutionError() != null) {
            error(scan.templateName, edge.line(), "Include target ["
                    + edge.literalTarget() + "] is invalid: " + edge.resolutionError());
            return;
        }

        String target = ((TemplateScan.StaticTarget) edge.target()).name();
        List<IncludeFrame> chain = append(site, scan.templateName, edge.line());
        analyze(scan(target),
                new IncludeSite(scan.templateName, edge.line(), chain));
    }

    private void analyzeComponentCall(TemplateScan owner, TemplateScan.ComponentCall call) {
        if (call.dynamicName()) {
            warning(owner.templateName, call.line(),
                    "Component name is dynamic and cannot be checked");
            return;
        }

        String path;
        try {
            path = components.resolvePath(call.name());
        } catch (IllegalArgumentException error) {
            error(owner.templateName, call.line(),
                    "Invalid component name: " + error.getMessage());
            return;
        }

        TemplateScan component = scan(path);
        if (component.failure instanceof TemplateScan.LoadFailure load) {
            error(owner.templateName, call.line(), "Component [" + call.name()
                    + "] template [" + path + "] could not be loaded: " + load.message());
            return;
        }
        if (component.failure != null) {
            analyze(component, null);
            return;
        }

        if (!call.dynamicProps()) {
            for (Map.Entry<String, Boolean> prop : component.props.entrySet()) {
                if (prop.getValue() && !call.props().contains(prop.getKey())) {
                    error(owner.templateName, call.line(), "Component [" + call.name()
                            + "] requires prop [" + prop.getKey() + "]");
                }
            }
        }
        for (String prop : call.props()) {
            if (!component.props.containsKey(prop)) {
                warning(owner.templateName, call.line(), "Component [" + call.name()
                        + "] does not declare prop [" + prop + "]");
            }
        }
        if (call.dynamicProps()) {
            warning(owner.templateName, call.line(), "Component [" + call.name()
                    + "] uses a dynamic prop name and cannot be checked");
        }
        analyze(component, null);
    }

    private void evaluate(TemplateScan scan, Scope scope, Site site) {
        evaluate(scan, scope, site, Set.of());
    }

    private void evaluate(TemplateScan scan, Scope scope, Site site,
            Set<String> overriddenBlocks) {
        if (scan.failure != null) {
            return;
        }
        if (includeStack.contains(scan.templateName)) {
            reportCycle(scan.templateName, site, "Include");
            return;
        }

        Scope effective = scope.retain(
                relevantNames.getOrDefault(scan.templateName, scope.names()));
        Set<String> ancestorNames = ancestorVisibleDeclarations(scan, overriddenBlocks);
        checkReads(scan, effective, site, overriddenBlocks, ancestorNames);
        Set<String> overrides = Set.copyOf(overriddenBlocks);
        if (!evaluated.add(new ContextKey(scan.templateName, effective.names(),
                effective.complete(), overrides))) {
            return;
        }
        int contexts = contextCounts.merge(scan.templateName, 1, Integer::sum);
        if (contexts > MAX_CONTEXTS_PER_TEMPLATE) {
            warning(scan.templateName, 0,
                    "Contract scope analysis truncated after "
                            + MAX_CONTEXTS_PER_TEMPLATE + " contexts");
            return;
        }
        evaluatedTemplates.add(scan.templateName);

        Scope blockScope = effective.plus(ancestorNames);
        includeStack.addLast(scan.templateName);
        try {
            analyze(scan, site);
            for (TemplateScan.IncludeEdge edge : scan.includes) {
                if (edge.block() != null && overrides.contains(edge.block())) {
                    continue;
                }
                evaluateInclude(scan, edge, edge.block() == null ? effective : blockScope, site);
            }
            for (TemplateScan.ComponentCall call : scan.componentCalls) {
                if (call.block() != null && overrides.contains(call.block())) {
                    continue;
                }
                evaluateComponent(scan, call);
            }
            evaluateParent(scan, effective, overrides);
        } finally {
            includeStack.removeLast();
        }
    }

    private void checkReads(TemplateScan scan, Scope scope, Site site,
            Set<String> overriddenBlocks, Set<String> ancestorNames) {
        if (model.allowsAny() || !scope.complete()) {
            return;
        }
        boolean unknownParent = hasUnknownParentScope(scan);
        for (TemplateScan.VariableRead read : scan.reads) {
            if (read.block() != null && overriddenBlocks.contains(read.block())) {
                continue;
            }
            if (read.block() != null && unknownParent) {
                // The parent may declare this name in its root body or in an
                // overridden block, so the read cannot be decided statically.
                continue;
            }
            if (isLocallyDeclared(scan, read, ancestorNames)
                    || model.allows(read.name())
                    || scope.names().contains(read.name())) {
                continue;
            }
            if (site == null) {
                error(scan.templateName, read.line(), "Variable [" + read.name()
                        + "] is not declared in the model schema");
            } else {
                error(site.callerTemplate(), site.line(), site.relation() + " template ["
                        + scan.templateName + ":" + read.line() + "] reads ["
                        + read.name() + "], which is not declared in the model schema "
                        + "or visible at this call"
                        + nestedChainSuffix(site.chain(), site.chainLabel()));
            }
        }
    }

    private boolean isLocallyDeclared(TemplateScan scan, TemplateScan.VariableRead read,
            Set<String> ancestorNames) {
        if (scan.rootDeclarations.contains(read.name())) {
            return true;
        }
        if (read.block() == null) {
            return false;
        }
        if (scan.blockDeclarations.getOrDefault(read.block(), Set.of()).contains(read.name())) {
            return true;
        }
        return ancestorNames.contains(read.name());
    }

    private boolean hasUnknownParentScope(TemplateScan scan) {
        Set<String> visited = new HashSet<>();
        TemplateScan current = scan;
        while (visited.add(current.templateName)) {
            TemplateScan.ParentCall parent = current.parent;
            if (parent == null) {
                return false;
            }
            if (parent.dynamicTarget() || parent.resolutionError() != null) {
                return true;
            }
            String target = parent.resolvedTarget();
            if (target == null) {
                return true;
            }
            TemplateScan parentScan = scan(target);
            if (parentScan.failure != null) {
                return true;
            }
            current = parentScan;
        }
        return false;
    }

    private void evaluateInclude(TemplateScan caller, TemplateScan.IncludeEdge edge,
            Scope callerScope, Site site) {
        if (!(edge.target() instanceof TemplateScan.StaticTarget target)
                || edge.resolutionError() != null) {
            return;
        }
        TemplateScan included = scan(target.name());
        if (included.failure != null) {
            return;
        }

        Scope child = callerScope
                .plus(edge.visibleAtCall())
                .plus(edge.withKeys());
        if (edge.dynamicWithKeys() || edge.scopeIncomplete()) {
            child = child.incomplete();
        }

        evaluate(included, child, new IncludeSite(caller.templateName, edge.line(),
                append(site, caller.templateName, edge.line())));
    }

    private void evaluateComponent(TemplateScan owner, TemplateScan.ComponentCall call) {
        if (call.dynamicName()) {
            return;
        }
        String path;
        try {
            path = components.resolvePath(call.name());
        } catch (IllegalArgumentException error) {
            return;
        }
        TemplateScan component = scan(path);
        if (component.failure != null) {
            return;
        }
        analyze(component, null);
        if (includeStack.contains(path)) {
            reportCycle(path, new IncludeSite(owner.templateName, call.line(), List.of()),
                    "Component");
            return;
        }
        evaluate(component, Scope.EMPTY, null);
    }

    private void evaluateParent(TemplateScan scan, Scope scope, Set<String> overriddenBlocks) {
        TemplateScan.ParentCall parent = scan.parent;
        if (parent == null || parent.dynamicTarget() || parent.resolutionError() != null) {
            return;
        }
        String target = parent.resolvedTarget();
        if (target == null) {
            return;
        }
        TemplateScan parentScan = scan(target);
        if (parentScan.failure != null) {
            return;
        }
        if (includeStack.contains(target)) {
            reportExtendsCycle(scan.templateName, parent.line(), target);
            return;
        }

        Scope parentScope = scope.plus(visibleDeclarations(scan, overriddenBlocks));
        Set<String> parentOverrides = new LinkedHashSet<>(overriddenBlocks);
        parentOverrides.addAll(scan.blockDeclarations.keySet());
        evaluate(parentScan, parentScope, null, parentOverrides);
    }

    private static Set<String> visibleDeclarations(TemplateScan scan,
            Set<String> overriddenBlocks) {
        Set<String> names = new LinkedHashSet<>(scan.rootDeclarations);
        for (Map.Entry<String, Set<String>> block : scan.blockDeclarations.entrySet()) {
            if (!overriddenBlocks.contains(block.getKey())) {
                names.addAll(block.getValue());
            }
        }
        return names;
    }

    private Set<String> ancestorVisibleDeclarations(TemplateScan scan,
            Set<String> overriddenBlocks) {
        Set<String> names = new LinkedHashSet<>();
        Set<String> overridden = new LinkedHashSet<>(overriddenBlocks);
        overridden.addAll(scan.blockDeclarations.keySet());
        Set<String> visited = new HashSet<>();
        visited.add(scan.templateName);
        TemplateScan current = scan;
        while (true) {
            String target = staticParent(current);
            if (target == null || !visited.add(target)) {
                return names;
            }
            TemplateScan parent = scan(target);
            if (parent.failure != null) {
                return names;
            }
            names.addAll(parent.rootDeclarations);
            for (Map.Entry<String, Set<String>> block : parent.blockDeclarations.entrySet()) {
                if (!overridden.contains(block.getKey())) {
                    names.addAll(block.getValue());
                }
            }
            overridden.addAll(parent.blockDeclarations.keySet());
            current = parent;
        }
    }

    private void reportExtendsCycle(String caller, int line, String target) {
        List<String> path = new ArrayList<>(includeStack);
        int index = path.indexOf(target);
        if (index < 0) {
            return;
        }
        List<String> cycle = new ArrayList<>(path.subList(index, path.size()));
        cycle.add(target);
        String canonical = canonicalCycle(cycle);
        if (reportedCycles.add("extends:" + canonical)) {
            error(caller, line, "Extends cycle: " + canonical);
        }
    }

    private void reportCycle(String target, Site site, String kind) {
        if (site == null) {
            return;
        }
        List<String> path = new ArrayList<>(includeStack);
        int index = path.indexOf(target);
        if (index < 0) {
            return;
        }
        List<String> cycle = new ArrayList<>(path.subList(index, path.size()));
        cycle.add(target);
        String canonical = canonicalCycle(cycle);
        if (reportedCycles.add(kind + ":" + canonical)) {
            error(site.callerTemplate(), site.line(), kind + " cycle: " + canonical);
        }
    }

    private void detectUnreachedCycles(SortedSet<String> universe) {
        Set<String> visited = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();
        for (String name : universe) {
            if (evaluatedTemplates.contains(name) || visited.contains(name)) {
                continue;
            }
            detectCycles(name, visited, path);
        }
    }

    private void detectCycles(String name, Set<String> visited, Deque<String> path) {
        if (!visited.add(name)) {
            return;
        }
        TemplateScan scan = scan(name);
        if (scan.failure != null) {
            return;
        }

        path.addLast(name);
        try {
            for (TemplateScan.IncludeEdge edge : scan.includes) {
                if (!(edge.target() instanceof TemplateScan.StaticTarget target)
                        || edge.resolutionError() != null) {
                    continue;
                }
                TemplateScan included = scan(target.name());
                if (included.failure != null) {
                    continue;
                }
                if (path.contains(target.name())) {
                    List<String> cyclePath = new ArrayList<>(path);
                    int index = cyclePath.indexOf(target.name());
                    List<String> cycle = new ArrayList<>(
                            cyclePath.subList(index, cyclePath.size()));
                    cycle.add(target.name());
                    String canonical = canonicalCycle(cycle);
                    if (reportedCycles.add("Include:" + canonical)) {
                        error(name, edge.line(), "Include cycle: " + canonical);
                    }
                } else if (!visited.contains(target.name())) {
                    detectCycles(target.name(), visited, path);
                }
            }
            String parent = staticParent(scan);
            if (parent != null) {
                TemplateScan parentScan = scan(parent);
                if (parentScan.failure == null) {
                    if (path.contains(parent)) {
                        List<String> cyclePath = new ArrayList<>(path);
                        int index = cyclePath.indexOf(parent);
                        List<String> cycle = new ArrayList<>(
                                cyclePath.subList(index, cyclePath.size()));
                        cycle.add(parent);
                        String canonical = canonicalCycle(cycle);
                        if (reportedCycles.add("extends:" + canonical)) {
                            error(name, scan.parent.line(), "Extends cycle: " + canonical);
                        }
                    } else if (!visited.contains(parent)) {
                        detectCycles(parent, visited, path);
                    }
                }
            }
        } finally {
            path.removeLast();
        }
    }

    private static String canonicalCycle(List<String> cycle) {
        int last = cycle.size() - 1;
        int smallest = 0;
        for (int i = 1; i < last; i++) {
            if (cycle.get(i).compareTo(cycle.get(smallest)) < 0) {
                smallest = i;
            }
        }
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < last; i++) {
            if (i > 0) {
                output.append(" -> ");
            }
            output.append(cycle.get((smallest + i) % last));
        }
        output.append(" -> ").append(cycle.get(smallest));
        return output.toString();
    }

    private static String targetLabel(TemplateScan.IncludeEdge edge) {
        return edge.literalTarget() == null ? "<dynamic>" : edge.literalTarget();
    }

    private static List<IncludeFrame> append(Site site, String template, int line) {
        List<IncludeFrame> chain = new ArrayList<>(
                site == null ? List.of() : site.chain());
        chain.add(new IncludeFrame(template, line));
        return List.copyOf(chain);
    }

    private static String nestedChainSuffix(List<IncludeFrame> chain, String label) {
        return chain.size() > 1 ? chainSuffix(chain, label) : "";
    }

    private static String chainSuffix(List<IncludeFrame> chain, String label) {
        if (chain.isEmpty()) {
            return "";
        }
        return " (" + label + " path: " + chain.stream()
                .map(frame -> frame.template() + ":" + frame.line())
                .collect(Collectors.joining(" -> ")) + ")";
    }

    private void error(String template, int line, String message) {
        diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.ERROR,
                template, line, message));
    }

    private void warning(String template, int line, String message) {
        diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.WARNING,
                template, line, message));
    }

    private ContractResult result() {
        return new ContractResult(new ArrayList<>(diagnostics));
    }

    private record IncludeFrame(String template, int line) {

        IncludeFrame {
            Objects.requireNonNull(template, "template");
        }
    }

    private sealed interface Site permits IncludeSite, ExtendsSite {

        String callerTemplate();

        int line();

        List<IncludeFrame> chain();

        String relation();

        String chainLabel();
    }

    private record IncludeSite(String callerTemplate, int line, List<IncludeFrame> chain)
            implements Site {

        IncludeSite {
            Objects.requireNonNull(callerTemplate, "callerTemplate");
            chain = List.copyOf(chain);
        }

        @Override
        public String relation() {
            return "Included";
        }

        @Override
        public String chainLabel() {
            return "include";
        }
    }

    private record ExtendsSite(String callerTemplate, int line, List<IncludeFrame> chain)
            implements Site {

        ExtendsSite {
            Objects.requireNonNull(callerTemplate, "callerTemplate");
            chain = List.copyOf(chain);
        }

        @Override
        public String relation() {
            return "Extended";
        }

        @Override
        public String chainLabel() {
            return "extends";
        }
    }

    private record ContextKey(String template, Set<String> names, boolean complete,
                              Set<String> overriddenBlocks) {
    }
}
