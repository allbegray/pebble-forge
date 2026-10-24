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
import java.util.LinkedHashSet;
import java.util.Set;
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

        ContractChecker checker = ContractChecker.builder()
                .loader(loader)
                .components(components)
                .model(schema)
                .build();

        Set<ContractDiagnostic> diagnostics = new LinkedHashSet<>();
        try (Stream<Path> paths = Files.walk(root.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .forEach(path -> {
                        String relative = root.toPath().relativize(path).toString()
                                .replace(File.separatorChar, '/');
                        String templateName = relative.substring(0,
                                relative.length() - suffix.length());
                        ContractResult result = checker.check(templateName);
                        diagnostics.addAll(result.diagnostics());
                    });
        } catch (IOException e) {
            throw new GradleException("Could not read templates from " + root, e);
        } catch (UncheckedIOException e) {
            throw new GradleException("Could not read a template under " + root, e);
        }

        boolean failed = false;
        for (ContractDiagnostic diagnostic : diagnostics) {
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
}
