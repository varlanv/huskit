package io.huskit.gradle.common.plugin.model.props.fake;

import io.huskit.gradle.common.plugin.model.props.NonNullProp;
import io.huskit.gradle.common.plugin.model.props.NullableProp;
import io.huskit.gradle.common.plugin.model.props.Props;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class FakeProps implements Props {

    private final Map<String, Object> props = new HashMap<>();
    private final Map<String, Object> envProps = new HashMap<>();

    @Override
    public boolean hasProp(CharSequence name) {
        return props.containsKey(name.toString());
    }

    @Override
    public NonNullProp nonnull(CharSequence name) {
        var nameString = name.toString();
        return new FakeNonNullProp(nameString, props.get(nameString));
    }

    @Override
    public NullableProp nullable(CharSequence name) {
        var nameString = name.toString();
        return new FakeNullableProp(nameString, props.get(nameString));
    }

    @Override
    public NullableProp env(CharSequence name) {
        var nameString = name.toString();
        return new FakeNullableProp(nameString, envProps.get(nameString));
    }

    public void add(CharSequence name, Object value) {
        props.put(name.toString(), value);
    }

    public void addEnv(CharSequence name, String value) {
        envProps.put(name.toString(), value);
    }
}
