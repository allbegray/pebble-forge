package io.github.allbegray.pebbleforge.gradle;

import io.github.allbegray.pebbleforge.PebbleForge;
import io.github.allbegray.pebbleforge.component.Components;
import io.github.allbegray.pebbleforge.diagnostics.DiagnosticReport;
import io.github.allbegray.pebbleforge.diagnostics.TemplateDiagnostics;
import io.github.allbegray.pebbleforge.trace.TemplateTrace;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.loader.FileLoader;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Renders one Pebble template with a model and prints source frames on failure.
 */
public abstract class RenderPebbleTemplateTask extends DefaultTask {

    /**
     * Creates the task and sets defaults for its options.
     */
    public RenderPebbleTemplateTask() {
        getTemplateSuffix().convention(".peb");
        getComponentRoot().convention("components");
        getStrictVariables().convention(true);
        getContextLines().convention(2);
        getModel().convention(Map.of());
    }

    /**
     * Returns the directory that contains the templates.
     *
     * @return the template directory
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getTemplatesDirectory();

    /**
     * Returns the template file suffix.
     *
     * @return the suffix, such as {@code .peb}
     */
    @Input
    public abstract Property<String> getTemplateSuffix();

    /**
     * Returns the template to render.
     *
     * @return the template name without the suffix
     */
    @Input
    @Optional
    public abstract Property<String> getTemplateName();

    /**
     * Returns the model values passed to the template.
     *
     * @return the model
     */
    @Input
    public abstract MapProperty<String, Object> getModel();

    /**
     * Returns the component root directory.
     *
     * @return the component root
     */
    @Input
    public abstract Property<String> getComponentRoot();

    /**
     * Returns whether missing variables fail the render.
     *
     * @return true when strict variables are enabled
     */
    @Input
    public abstract Property<Boolean> getStrictVariables();

    /**
     * Returns the number of source lines shown around an error.
     *
     * @return the context line count
     */
    @Input
    public abstract Property<Integer> getContextLines();

    /**
     * Sets the template to render from the command line.
     *
     * @param templateName the template name without the suffix
     */
    @Option(option = "template", description = "Template name to render, without the suffix.")
    public void setTemplateOption(String templateName) {
        getTemplateName().set(templateName);
    }

    /**
     * Renders the template and prints the output, or prints source frames on failure.
     */
    @TaskAction
    public void renderTemplate() {
        File root = getTemplatesDirectory().get().getAsFile();
        if (!root.isDirectory()) {
            throw new GradleException("Template directory does not exist: " + root);
        }

        String templateName = getTemplateName().getOrNull();
        if (templateName == null || templateName.isBlank()) {
            throw new GradleException("pebbleForgeDiagnostics.templateName is required. "
                    + "Set a template name relative to the template root without the suffix.");
        }

        String suffix = getTemplateSuffix().get();
        Path rootPath = root.toPath().toAbsolutePath().normalize();
        Path templateFile = rootPath.resolve(templateName + suffix).normalize();
        if (!templateFile.startsWith(rootPath) || !Files.isRegularFile(templateFile)) {
            throw new GradleException("Pebble template [" + templateName + "] does not exist at "
                    + templateFile + ". Set templateName relative to the template root without the "
                    + suffix + " suffix.");
        }

        FileLoader loader = new FileLoader(root.getAbsolutePath());
        loader.setSuffix(suffix);

        Components components = Components.builder()
                .root(getComponentRoot().get())
                .suffix("")
                .build();

        PebbleEngine engine = PebbleForge.engine()
                .loader(loader)
                .components(components)
                .strictVariables(getStrictVariables().get())
                .build();

        TemplateDiagnostics diagnostics = TemplateDiagnostics.builder()
                .loader(loader)
                .contextLines(getContextLines().get())
                .build();

        StringWriter output = new StringWriter();
        try {
            TemplateTrace.run(() -> engine.getTemplate(templateName)
                    .evaluate(output, getModel().get()));
        } catch (PebbleException error) {
            DiagnosticReport report = diagnostics.render(error);
            getLogger().error(report.format());
            throw new GradleException(
                    "Pebble template [" + templateName + "] failed to render", error);
        } catch (IOException error) {
            throw new GradleException(
                    "Could not render Pebble template [" + templateName + "]", error);
        }

        getLogger().lifecycle(output.toString());
    }
}
