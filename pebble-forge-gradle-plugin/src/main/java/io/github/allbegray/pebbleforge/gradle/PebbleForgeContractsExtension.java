package io.github.allbegray.pebbleforge.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

/**
 * Configuration for the Pebble Forge contract check task.
 */
public abstract class PebbleForgeContractsExtension {

    public abstract DirectoryProperty getTemplatesDirectory();

    public abstract Property<String> getTemplateSuffix();

    public abstract Property<String> getComponentRoot();

    public abstract SetProperty<String> getVariables();

    public abstract Property<Boolean> getAllowAnyVariable();
}
