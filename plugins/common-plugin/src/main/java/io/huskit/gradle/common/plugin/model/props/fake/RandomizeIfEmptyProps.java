package io.huskit.gradle.common.plugin.model.props.fake;

import io.huskit.gradle.common.plugin.model.props.NonNullProp;
import io.huskit.gradle.common.plugin.model.props.NullableProp;
import io.huskit.gradle.common.plugin.model.props.Props;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class RandomizeIfEmptyProps implements Props {

    private final Props delegate;

    @Override
    public boolean hasProp(CharSequence name) {
        return true;
    }

    @Override
    public NonNullProp nonnull(CharSequence name) {
        var nameString = name.toString();
        if (!delegate.hasProp(nameString)) {
            String value = UUID.randomUUID().toString();
            return new FakeNonNullProp(nameString, value);
        }
        return delegate.nonnull(nameString);
    }

    @Override
    public NullableProp nullable(CharSequence name) {
        var nameString = name.toString();
        if (!delegate.hasProp(nameString)) {
            String value = UUID.randomUUID().toString();
            return new FakeNullableProp(nameString, value);
        }
        return delegate.nullable(nameString);
    }

    @Override
    public NullableProp env(CharSequence name) {
        var nameString = name.toString();
        NullableProp env = delegate.env(nameString);
        if (env.value() == null) {
            return new FakeNullableProp(nameString, UUID.randomUUID().toString());
        }
        return env;
    }
}
