package io.huskit.gradle.plugin;

import org.gradle.api.provider.Property;

public interface InternalConventionExtension {

    static String name() {
        return "internalConvention";
    }

    Property<String> getIntegrationTestName();

    Property<Boolean> getInternalModule();
}
