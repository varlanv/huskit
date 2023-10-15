package io.huskit.gradle.common.plugin.model.props;

import io.huskit.gradle.common.plugin.model.NewOrExistingExtension;
import lombok.RequiredArgsConstructor;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.provider.ProviderFactory;

import java.util.List;

@RequiredArgsConstructor
public class ReplaceableProps implements Props {

    private final ProviderFactory providers;
    private final ExtraPropertiesExtension extraPropertiesExtension;
    private final NewOrExistingExtension newOrExistingExtension;
    private Props delegate;

    @Override
    public boolean hasProp(CharSequence name) {
        return getDelegate().hasProp(name);
    }

    @Override
    public NonNullProp nonnull(CharSequence name) {
        return getDelegate().nonnull(name);
    }

    @Override
    public NullableProp nullable(CharSequence name) {
        return getDelegate().nullable(name);
    }

    @Override
    public NullableProp env(CharSequence name) {
        return getDelegate().env(name);
    }

    private Props getDelegate() {
        if (delegate == null) {
            delegate = newOrExistingExtension.getOrCreate(
                    Props.class,
                    DefaultProps.class,
                    Props.EXTENSION_NAME,
                    () -> List.of(
                            providers,
                            extraPropertiesExtension
                    )
            );
        }
        return delegate;
    }
}
