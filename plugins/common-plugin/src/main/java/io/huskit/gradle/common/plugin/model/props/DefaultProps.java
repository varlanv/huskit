package io.huskit.gradle.common.plugin.model.props;

import lombok.RequiredArgsConstructor;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.provider.ProviderFactory;

@RequiredArgsConstructor
public class DefaultProps implements Props {

    private final ProviderFactory providers;
    private final ExtraPropertiesExtension extraPropertiesExtension;

    @Override
    public boolean hasProp(CharSequence name) {
        return nullable(name).value() != null;
    }

    @Override
    public NonNullProp nonnull(CharSequence name) {
        return new DefaultNonNullProp(nullable(name));
    }

    @Override
    public NullableProp nullable(CharSequence name) {
        var nameString = name.toString();
        return new DefaultNullableProp(
                nameString,
                providers.gradleProperty(nameString).orElse(
                        providers.provider(() -> {
                            if (extraPropertiesExtension.has(nameString)) {
                                return extraPropertiesExtension.get(nameString);
                            } else {
                                return null;
                            }
                        }).map(Object::toString))
        );
    }

    @Override
    public NullableProp env(CharSequence name) {
        var nameString = name.toString();
        return new DefaultNullableProp(
                nameString,
                providers.environmentVariable(nameString)
        );
    }
}
