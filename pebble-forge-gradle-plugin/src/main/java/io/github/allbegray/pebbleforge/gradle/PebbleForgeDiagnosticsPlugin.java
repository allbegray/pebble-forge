package io.github.allbegray.pebbleforge.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.Map;

/**
 * Registers the renderPebbleTemplate task.
 */
public final class PebbleForgeDiagnosticsPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "pebbleForgeDiagnostics";
    public static final String TASK_NAME = "renderPebbleTemplate";

    @Override
    public void apply(Project project) {
        PebbleForgeDiagnosticsExtension extension = project.getExtensions()
                .create(EXTENSION_NAME, PebbleForgeDiagnosticsExtension.class);
        extension.getTemplatesDirectory().convention(
                project.getLayout().getProjectDirectory().dir("src/main/resources/templates"));
        extension.getTemplateSuffix().convention(".peb");
        extension.getModel().convention(Map.of());
        extension.getComponentRoot().convention("components");
        extension.getStrictVariables().convention(true);
        extension.getContextLines().convention(2);

        project.getTasks()
                .register(TASK_NAME, RenderPebbleTemplateTask.class, render -> {
                    render.setGroup("help");
                    render.setDescription(
                            "Renders one Pebble template and prints source-frame diagnostics.");
                    render.getTemplatesDirectory().set(extension.getTemplatesDirectory());
                    render.getTemplateSuffix().set(extension.getTemplateSuffix());
                    render.getTemplateName().set(extension.getTemplateName());
                    render.getModel().set(extension.getModel());
                    render.getComponentRoot().set(extension.getComponentRoot());
                    render.getStrictVariables().set(extension.getStrictVariables());
                    render.getContextLines().set(extension.getContextLines());
                });
    }
}
