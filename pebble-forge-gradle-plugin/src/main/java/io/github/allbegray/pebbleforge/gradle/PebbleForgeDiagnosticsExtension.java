package io.github.allbegray.pebbleforge.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration for rendering one Pebble template with source-frame diagnostics.
 */
public abstract class PebbleForgeDiagnosticsExtension {

    public abstract DirectoryProperty getTemplatesDirectory();

    public abstract Property<String> getTemplateSuffix();

    public abstract Property<String> getTemplateName();

    public abstract MapProperty<String, Object> getModel();

    public abstract Property<String> getComponentRoot();

    public abstract Property<Boolean> getStrictVariables();

    public abstract Property<Integer> getContextLines();
}
