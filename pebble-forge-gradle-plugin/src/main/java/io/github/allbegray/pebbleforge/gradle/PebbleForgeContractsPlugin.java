package io.github.allbegray.pebbleforge.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

/**
 * Registers the checkPebbleTemplates task.
 */
public final class PebbleForgeContractsPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "pebbleForgeContracts";
    public static final String TASK_NAME = "checkPebbleTemplates";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("base");

        PebbleForgeContractsExtension extension = project.getExtensions()
                .create(EXTENSION_NAME, PebbleForgeContractsExtension.class);
        extension.getTemplatesDirectory().convention(
                project.getLayout().getProjectDirectory().dir("src/main/resources/templates"));
        extension.getTemplateSuffix().convention(".peb");
        extension.getComponentRoot().convention("components");
        extension.getAllowAnyVariable().convention(false);

        TaskProvider<CheckPebbleTemplatesTask> task = project.getTasks()
                .register(TASK_NAME, CheckPebbleTemplatesTask.class, check -> {
                    check.setGroup("verification");
                    check.setDescription("Checks Pebble templates against the model schema.");
                    check.getTemplatesDirectory().set(extension.getTemplatesDirectory());
                    check.getTemplateSuffix().set(extension.getTemplateSuffix());
                    check.getComponentRoot().set(extension.getComponentRoot());
                    check.getVariables().set(extension.getVariables());
                    check.getAllowAnyVariable().set(extension.getAllowAnyVariable());
                });

        project.getTasks().named("check").configure(check -> check.dependsOn(task));
    }
}
