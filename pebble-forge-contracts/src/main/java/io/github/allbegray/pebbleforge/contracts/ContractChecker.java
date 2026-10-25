package io.github.allbegray.pebbleforge.contracts;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.component.Components;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.LoaderException;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Checks template variable reads and component prop calls against a model schema.
 */
public final class ContractChecker {

    private final PebbleEngine engine;
    private final Components components;
    private final ModelSchema model;

    private ContractChecker(Builder builder) {
        this.components = builder.components;
        this.model = builder.model;
        this.engine = PebbleForge.engine()
                .loader(builder.loader)
                .components(builder.components)
                .languageExtensions(true)
                .strictVariables(false)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public ContractResult check(String templateName) {
        List<ContractDiagnostic> diagnostics = new ArrayList<>();
        TemplateScan scan = scan(templateName, new HashMap<>(), diagnostics);
        if (scan.loadError != null) {
            diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.ERROR,
                    templateName, 0, "Could not load template: " + scan.loadError.getMessage()));
        }
        return new ContractResult(diagnostics);
    }

    private TemplateScan scan(String templateName, Map<String, TemplateScan> scans,
            List<ContractDiagnostic> diagnostics) {
        TemplateScan existing = scans.get(templateName);
        if (existing != null) {
            return existing;
        }

        TemplateScan scan = new TemplateScan(templateName);
        scans.put(templateName, scan);

        try {
            PebbleTemplate template = engine.getTemplate(templateName);
            if (!(template instanceof PebbleTemplateImpl implementation)) {
                diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.WARNING,
                        templateName, 0, "Template is not a PebbleTemplateImpl and was skipped"));
                return scan;
            }

            implementation.getRootNode().accept(new ContractVisitor(scan, implementation));
            reportMissingVariables(scan, diagnostics);

            for (TemplateScan.ComponentCall call : scan.componentCalls) {
                validateComponentCall(call, scan.templateName, scans, diagnostics);
            }
        } catch (LoaderException e) {
            scan.failed = true;
            scan.loadError = e;
        } catch (PebbleException e) {
            scan.failed = true;
            diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.ERROR,
                    templateName, lineOf(e),
                    "Could not parse template: " + e.getMessage()));
        } finally {
            scan.complete = true;
        }

        return scan;
    }

    private static int lineOf(PebbleException error) {
        Integer line = error.getLineNumber();
        return line == null ? 0 : line;
    }

    private void reportMissingVariables(TemplateScan scan, List<ContractDiagnostic> diagnostics) {
        if (model.allowsAny()) {
            return;
        }
        for (TemplateScan.VariableRead read : scan.reads) {
            if (scan.declaredVariables.contains(read.name()) || model.allows(read.name())) {
                continue;
            }
            diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.ERROR,
                    scan.templateName, read.line(),
                    "Variable [" + read.name() + "] is not declared in the model schema"));
        }
    }

    private void validateComponentCall(TemplateScan.ComponentCall call, String templateName,
            Map<String, TemplateScan> scans, List<ContractDiagnostic> diagnostics) {
        if (call.dynamicName()) {
            diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.WARNING,
                    templateName, call.line(),
                    "Component name is dynamic and cannot be checked"));
            return;
        }

        String componentPath;
        try {
            componentPath = components.resolvePath(call.name());
        } catch (IllegalArgumentException e) {
            diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.ERROR,
                    templateName, call.line(), "Invalid component name: " + e.getMessage()));
            return;
        }

        TemplateScan component = scan(componentPath, scans, diagnostics);
        if (component.loadError != null) {
            diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.ERROR,
                    templateName, call.line(),
                    "Component [" + call.name() + "] template [" + componentPath
                            + "] could not be loaded: " + component.loadError.getMessage()));
            return;
        }
        if (!component.complete || component.failed) {
            return;
        }

        for (Map.Entry<String, Boolean> prop : component.props.entrySet()) {
            if (prop.getValue() && !call.props().contains(prop.getKey())) {
                diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.ERROR,
                        templateName, call.line(),
                        "Component [" + call.name() + "] requires prop [" + prop.getKey() + "]"));
            }
        }
        for (String prop : call.props()) {
            if (!component.props.containsKey(prop)) {
                diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.WARNING,
                        templateName, call.line(),
                        "Component [" + call.name() + "] does not declare prop [" + prop + "]"));
            }
        }
        if (call.dynamicProps()) {
            diagnostics.add(new ContractDiagnostic(ContractDiagnostic.Severity.WARNING,
                    templateName, call.line(),
                    "Component [" + call.name() + "] uses a dynamic prop name and cannot be checked"));
        }
    }

    public static final class Builder {

        private Loader<?> loader;
        private Components components = Components.defaults();
        private ModelSchema model = ModelSchema.any();

        public Builder loader(Loader<?> loader) {
            this.loader = Objects.requireNonNull(loader, "loader");
            return this;
        }

        public Builder components(Components components) {
            this.components = Objects.requireNonNull(components, "components");
            return this;
        }

        public Builder model(ModelSchema model) {
            this.model = Objects.requireNonNull(model, "model");
            return this;
        }

        public ContractChecker build() {
            if (loader == null) {
                throw new IllegalStateException("A loader is required");
            }
            return new ContractChecker(this);
        }
    }
}
