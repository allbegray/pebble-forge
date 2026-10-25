package io.github.allbegray.pebbleforge.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration for rendering one Pebble template with source-frame diagnostics.
 */
public abstract class PebbleForgeDiagnosticsExtension {

    /**
     * Creates the extension. Gradle instantiates it.
     */
    public PebbleForgeDiagnosticsExtension() {
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
     * Returns the template to render.
     *
     * @return the template name without the suffix
     */
    public abstract Property<String> getTemplateName();

    /**
     * Returns the model values passed to the template.
     *
     * @return the model
     */
    public abstract MapProperty<String, Object> getModel();

    /**
     * Returns the component root directory.
     *
     * @return the component root
     */
    public abstract Property<String> getComponentRoot();

    /**
     * Returns whether missing variables fail the render.
     *
     * @return true when strict variables are enabled
     */
    public abstract Property<Boolean> getStrictVariables();

    /**
     * Returns the number of source lines shown around an error.
     *
     * @return the context line count
     */
    public abstract Property<Integer> getContextLines();
}
