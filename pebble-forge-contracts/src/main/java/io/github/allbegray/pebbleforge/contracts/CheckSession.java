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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

final class CheckSession {

    private final PebbleEngine engine;
    private final Components components;
    private final ModelSchema model;
    private final Set<String> rootTemplates;
    private final Set<String> partialTemplates;

    private final Map<String, TemplateScan> scans = new TreeMap<>();
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
        analyze(scan, null);
        evaluate(scan, Scope.EMPTY, null);
        return result();
    }

    ContractResult checkAll(Collection<String> templateNames) {
        SortedSet<String> universe = new TreeSet<>();
        for (String name : templateNames) {
            universe.add(Objects.requireNonNull(name, "templateName"));
        }
        for (String name : universe) {
            scan(name);
        }
        for (String name : rootTemplates) {
            scan(name);
        }

        SortedSet<String> roots = inferRoots(universe);
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

    private SortedSet<String> inferRoots(SortedSet<String> universe) {
        Set<String> incoming = new HashSet<>();
        for (TemplateScan scan : scans.values()) {
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
            } else if (!incoming.contains(name)) {
                roots.add(name);
            }
        }
        roots.addAll(rootTemplates);
        return roots;
    }

    private boolean isComponent(String name) {
        String root = components.root();
        return !root.isEmpty() && name.startsWith(root + "/");
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

    private void analyze(TemplateScan scan, IncludeSite site) {
        if (scan.failure instanceof TemplateScan.LoadFailure) {
            reportFailure(scan, site);
            return;
        }
        if (!analyzed.add(scan.templateName)) {
            return;
        }
        if (scan.failure != null) {
            reportFailure(scan, site);
            return;
        }
        for (TemplateScan.IncludeEdge edge : scan.includes) {
            analyzeInclude(scan, edge, site);
        }
        for (TemplateScan.ComponentCall call : scan.componentCalls) {
            analyzeComponentCall(scan, call);
        }
    }

    private void reportFailure(TemplateScan scan, IncludeSite site) {
        if (scan.failure instanceof TemplateScan.LoadFailure load) {
            if (site == null) {
                error(scan.templateName, 0, "Could not load template: " + load.message());
            } else {
                error(site.callerTemplate(), site.line(), "Included template ["
                        + scan.templateName + "] was not found: " + load.message()
                        + chainSuffix(site.chain()));
            }
            return;
        }
        TemplateScan.ParseFailure parse = (TemplateScan.ParseFailure) scan.failure;
        if (site == null) {
            error(scan.templateName, parse.line(),
                    "Could not parse template: " + parse.message());
        } else {
            error(scan.templateName, parse.line(), "Included template ["
                    + scan.templateName + "] could not be parsed: " + parse.message()
                    + chainSuffix(site.chain()));
        }
    }

    private void analyzeInclude(TemplateScan scan, TemplateScan.IncludeEdge edge,
            IncludeSite site) {
        if (edge.target() instanceof TemplateScan.DynamicTarget) {
            warning(scan.templateName, edge.line(),
                    "Include target is dynamic and cannot be checked");
        }
        if (edge.dynamicWithKeys()) {
            warning(scan.templateName, edge.line(), "Include [" + targetLabel(edge)
                    + "] uses a dynamic with key; variables in the included template "
                    + "cannot be fully checked");
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

        for (Map.Entry<String, Boolean> prop : component.props.entrySet()) {
            if (prop.getValue() && !call.props().contains(prop.getKey())) {
                error(owner.templateName, call.line(), "Component [" + call.name()
                        + "] requires prop [" + prop.getKey() + "]");
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

    private void evaluate(TemplateScan scan, Scope scope, IncludeSite site) {
        if (scan.failure != null) {
            return;
        }
        if (includeStack.contains(scan.templateName)) {
            reportCycle(scan.templateName, site);
            return;
        }

        checkReads(scan, scope, site);
        if (!evaluated.add(new ContextKey(scan.templateName, scope.names(),
                scope.complete()))) {
            return;
        }
        evaluatedTemplates.add(scan.templateName);

        includeStack.addLast(scan.templateName);
        try {
            analyze(scan, site);
            for (TemplateScan.IncludeEdge edge : scan.includes) {
                evaluateInclude(scan, edge, scope, site);
            }
            for (TemplateScan.ComponentCall call : scan.componentCalls) {
                evaluateComponent(call);
            }
            evaluateParent(scan, scope);
        } finally {
            includeStack.removeLast();
        }
    }

    private void checkReads(TemplateScan scan, Scope scope, IncludeSite site) {
        if (model.allowsAny() || !scope.complete()) {
            return;
        }
        for (TemplateScan.VariableRead read : scan.reads) {
            if (scan.declaredVariables.contains(read.name())
                    || model.allows(read.name())
                    || scope.names().contains(read.name())) {
                continue;
            }
            if (site == null) {
                error(scan.templateName, read.line(), "Variable [" + read.name()
                        + "] is not declared in the model schema");
            } else {
                error(site.callerTemplate(), site.line(), "Included template ["
                        + scan.templateName + ":" + read.line() + "] reads ["
                        + read.name() + "], which is not declared in the model schema "
                        + "or visible at this include" + nestedChainSuffix(site.chain()));
            }
        }
    }

    private void evaluateInclude(TemplateScan caller, TemplateScan.IncludeEdge edge,
            Scope callerScope, IncludeSite site) {
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
        if (edge.dynamicWithKeys()) {
            child = child.incomplete();
        }

        evaluate(included, child, new IncludeSite(caller.templateName, edge.line(),
                append(site, caller.templateName, edge.line())));
    }

    private void evaluateComponent(TemplateScan.ComponentCall call) {
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
        evaluate(component, Scope.EMPTY, null);
    }

    private void evaluateParent(TemplateScan scan, Scope scope) {
        // EXTENDS SEAM: ParentCall is recorded, but parent propagation lands here.
    }

    private void reportCycle(String target, IncludeSite site) {
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
        if (reportedCycles.add(canonical)) {
            error(site.callerTemplate(), site.line(), "Include cycle: " + canonical);
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
                    if (reportedCycles.add(canonical)) {
                        error(name, edge.line(), "Include cycle: " + canonical);
                    }
                } else if (!visited.contains(target.name())) {
                    detectCycles(target.name(), visited, path);
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

    private static List<IncludeFrame> append(IncludeSite site, String template, int line) {
        List<IncludeFrame> chain = new ArrayList<>(
                site == null ? List.of() : site.chain());
        chain.add(new IncludeFrame(template, line));
        return List.copyOf(chain);
    }

    private static String nestedChainSuffix(List<IncludeFrame> chain) {
        return chain.size() > 1 ? chainSuffix(chain) : "";
    }

    private static String chainSuffix(List<IncludeFrame> chain) {
        if (chain.isEmpty()) {
            return "";
        }
        return " (include path: " + chain.stream()
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

    private record IncludeSite(String callerTemplate, int line, List<IncludeFrame> chain) {

        IncludeSite {
            Objects.requireNonNull(callerTemplate, "callerTemplate");
            chain = List.copyOf(chain);
        }
    }

    private record ContextKey(String template, Set<String> names, boolean complete) {
    }
}
