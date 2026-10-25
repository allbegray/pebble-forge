package io.github.allbegray.pebbleforge.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckPebbleTemplatesTaskTest {

    @Test
    void directRegistrationUsesEmptyConventions() {
        Project project = ProjectBuilder.builder().build();

        CheckPebbleTemplatesTask task = project.getTasks()
                .create("directCheck", CheckPebbleTemplatesTask.class);

        assertThat(task.getComponentRoots().get()).isEmpty();
        assertThat(task.getRootTemplates().get()).isEmpty();
        assertThat(task.getPartialTemplates().get()).isEmpty();
    }
}
