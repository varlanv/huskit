package io.huskit.gradle.common.plugin.model.props.fake;

import io.huskit.gradle.common.plugin.model.props.exception.NonNullPropertyException;
import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class FakeNonNullPropTest implements UnitTest {

    String propName = "propName";
    String propValue = "propVal";

    @Test
    @DisplayName("'name' should return param")
    void name_should_return_param() {
        assertThat(new FakeNonNullProp(propName, propValue).name()).isEqualTo(propName);
    }

    @Test
    @DisplayName("'value' if value non null should return param")
    void value_if_value_non_null_should_return_param() {
        assertThat(new FakeNonNullProp(propName, propValue).value()).isEqualTo(propValue);
    }

    @Test
    @DisplayName("'stringValue' if value non null should return param")
    void stringvalue_if_value_non_null_should_return_param() {
        assertThat(new FakeNonNullProp(propName, propValue).stringValue()).isEqualTo(propValue);
    }

    @Test
    @DisplayName("'value' if value null then throw exception")
    void value_if_value_null_then_throw_exception() {
        assertThatThrownBy(() -> new FakeNonNullProp(propName, null).value())
                .isInstanceOf(NonNullPropertyException.class)
                .hasMessageContaining(propName);
    }

    @Test
    @DisplayName("'stringValue' if value null then throw exception")
    void stringvalue_if_value_null_then_throw_exception() {
        assertThatThrownBy(() -> new FakeNonNullProp(propName, null).stringValue())
                .isInstanceOf(NonNullPropertyException.class)
                .hasMessageContaining(propName);
    }
}
