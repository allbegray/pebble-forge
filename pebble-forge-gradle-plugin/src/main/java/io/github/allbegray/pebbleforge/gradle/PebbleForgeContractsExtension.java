package io.github.allbegray.pebbleforge.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

/**
 * Configuration for the Pebble Forge contract check task.
 */
public abstract class PebbleForgeContractsExtension {

    /**
     * Creates the extension. Gradle instantiates it.
     */
    public PebbleForgeContractsExtension() {
    }

    /**
     * Returns the directory that contains the templates.
     *
     * @return the template directory
     */
    public abstract DirectoryProperty getTemplatesDirectory();

    /**
     * Returns the template file suffix.
     *
     * @return the suffix, such as {@code .peb}
     */
    public abstract Property<String> getTemplateSuffix();

    /**
     * Returns the component root directory.
     *
     * @return the component root
     */
    public abstract Property<String> getComponentRoot();

    /**
     * Returns namespace-to-root-directory mappings for named component roots.
     *
     * @return the named component roots
     */
    public abstract MapProperty<String, String> getComponentRoots();

    /**
     * Returns the model variables that templates may read.
     *
     * @return the variable names
     */
    public abstract SetProperty<String> getVariables();

    /**
     * Returns the optional JSON or YAML schema file with model variables.
     *
     * @return the schema file
     */
    public abstract RegularFileProperty getSchemaFile();

    /**
     * Returns whether every model variable is allowed.
     *
     * @return true when the model schema allows any variable
     */
    public abstract Property<Boolean> getAllowAnyVariable();

    /**
     * Returns the templates to check as roots.
     *
     * @return the root template names
     */
    public abstract SetProperty<String> getRootTemplates();

    /**
     * Returns the templates to keep out of root inference.
     *
     * @return the partial template names
     */
    public abstract SetProperty<String> getPartialTemplates();
}
