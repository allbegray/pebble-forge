package io.github.allbegray.pebbleforge.gradle;

import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.contracts.ContractChecker;
import io.github.allbegray.pebbleforge.contracts.ContractDiagnostic;
import io.github.allbegray.pebbleforge.contracts.ContractResult;
import io.github.allbegray.pebbleforge.contracts.ModelSchema;
import io.pebbletemplates.pebble.loader.FileLoader;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Checks every template under the configured root.
 */
public abstract class CheckPebbleTemplatesTask extends DefaultTask {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getTemplatesDirectory();

    @Input
    public abstract Property<String> getTemplateSuffix();

    @Input
    public abstract Property<String> getComponentRoot();

    @Input
    public abstract SetProperty<String> getVariables();

    @Input
    public abstract Property<Boolean> getAllowAnyVariable();

    @Input
    public abstract SetProperty<String> getRootTemplates();

    @Input
    public abstract SetProperty<String> getPartialTemplates();

    @TaskAction
    public void checkTemplates() {
        File root = getTemplatesDirectory().get().getAsFile();
        if (!root.isDirectory()) {
            throw new GradleException("Template directory does not exist: " + root);
        }

        String suffix = getTemplateSuffix().get();
        FileLoader loader = new FileLoader(root.getAbsolutePath());
        loader.setSuffix(suffix);

        Components components = Components.builder()
                .root(getComponentRoot().get())
                .suffix("")
                .build();

        ModelSchema schema = getAllowAnyVariable().get()
                ? ModelSchema.any()
                : ModelSchema.builder()
                        .variables(getVariables().get().toArray(String[]::new))
                        .build();

        Set<String> templateNames = templateNames(root.toPath(), suffix);
        Set<String> rootTemplates = configuredTemplates(
                "rootTemplates", getRootTemplates().get(), templateNames);
        Set<String> partialTemplates = configuredTemplates(
                "partialTemplates", getPartialTemplates().get(), templateNames);

        String componentPrefix = components.root().isEmpty()
                ? null
                : components.root() + "/";
        for (String name : partialTemplates) {
            if (componentPrefix != null && name.startsWith(componentPrefix)) {
                getLogger().warn("Template [{}] is listed in partialTemplates but belongs "
                                + "to the component root [{}]; it is checked as a component.",
                        name, components.root());
            }
        }

        ContractChecker checker = ContractChecker.builder()
                .loader(loader)
                .components(components)
                .model(schema)
                .rootTemplates(rootTemplates)
                .partialTemplates(partialTemplates)
                .build();

        ContractResult result = checker.checkAll(templateNames);

        boolean failed = false;
        for (ContractDiagnostic diagnostic : result.diagnostics()) {
            if (diagnostic.severity() == ContractDiagnostic.Severity.ERROR) {
                getLogger().error(diagnostic.toString());
                failed = true;
            } else {
                getLogger().warn(diagnostic.toString());
            }
        }

        if (failed) {
            throw new GradleException("Pebble template contract check failed");
        }
        getLogger().info("Checked Pebble templates under {}", root);
    }

    private static Set<String> templateNames(Path root, String suffix) {
        Set<String> names = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String relative = root.relativize(path).toString()
                                .replace(File.separatorChar, '/');
                        if (relative.endsWith(suffix)) {
                            names.add(relative.substring(0,
                                    relative.length() - suffix.length()));
                        }
                    });
        } catch (IOException e) {
            throw new GradleException("Could not read templates from " + root, e);
        } catch (UncheckedIOException e) {
            throw new GradleException("Could not read a template under " + root, e);
        }
        return names;
    }

    private static Set<String> configuredTemplates(
            String label, Set<String> configured, Set<String> known) {
        Set<String> names = new TreeSet<>();
        for (String value : configured) {
            String name = value.trim();
            if (name.isEmpty()) {
                throw new GradleException(label + " must not contain an empty template name");
            }
            if (!known.contains(name)) {
                throw new GradleException("Unknown template [" + name + "] in " + label
                        + ". Configure names relative to the template root with the "
                        + "template suffix removed.");
            }
            names.add(name);
        }
        return names;
    }
}
