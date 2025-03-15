package io.huskit.gradle.common.plugin.model.props;

import io.huskit.gradle.commontest.GradleIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultNullablePropIntegrationTest implements GradleIntegrationTest {

    String propName = "anyPropName";
    String propVal = "anyPropVal";

    @Test
    @DisplayName("'name' should return prop name")
    void name_should_return_prop_name() {
        runProjectFixture(fixture -> {
            var project = fixture.project();
            var subject = new DefaultNullableProp(propName, project.getProviders().provider(() -> propVal));

            assertThat(subject.name()).isEqualTo(propName);
        });
    }

    @Test
    @DisplayName("'value' should return prop value")
    void value_should_return_prop_value() {
        runProjectFixture(fixture -> {
            var project = fixture.project();
            var subject = new DefaultNullableProp(propName, project.getProviders().provider(() -> propVal));

            assertThat(subject.value()).isEqualTo(propVal);
        });
    }

    @Test
    @DisplayName("'stringValue' should return prop value")
    void stringvalue_should_return_prop_value() {
        runProjectFixture(fixture -> {
            var project = fixture.project();
            var subject = new DefaultNullableProp(propName, project.getProviders().provider(() -> propVal));

            assertThat(subject.stringValue()).isEqualTo(propVal);
        });
    }

    @MethodSource("holdsTrueShouldReturnTrueIfPropValueIsTrue")
    @ParameterizedTest
    @DisplayName("'holdsTrue' should return true if prop value is true")
    void holdstrue_should_return_true_if_prop_value_is_true(String truth, boolean expected) {
        runProjectFixture(fixture -> {
            var project = fixture.project();
            var subject = new DefaultNullableProp(propName, project.getProviders().provider(() -> truth));

            assertThat(subject.holdsTrue()).isEqualTo(expected);
        });
    }

    static Stream<Arguments> holdsTrueShouldReturnTrueIfPropValueIsTrue() {
        return Stream.of(
                Arguments.of("tRuE", true),
                Arguments.of("true", true),
                Arguments.of("TRUE", true),
                Arguments.of("false", false),
                Arguments.of(null, false),
                Arguments.of("", false)
        );
    }

    @MethodSource("holdsFalseShouldReturnTrueIfPropValueIsFalse")
    @ParameterizedTest
    @DisplayName("'holdsFalse' should return true if prop value is false")
    void holdsfalse_should_return_true_if_prop_value_is_false(String truth, boolean expected) {
        runProjectFixture(fixture -> {
            var project = fixture.project();
            var subject = new DefaultNullableProp(propName, project.getProviders().provider(() -> truth));

            assertThat(subject.holdsFalse()).isEqualTo(expected);
        });
    }

    static Stream<Arguments> holdsFalseShouldReturnTrueIfPropValueIsFalse() {
        return Stream.of(
                Arguments.of("false", true),
                Arguments.of("FALSE", true),
                Arguments.of("FaLsE", true),
                Arguments.of("true", false),
                Arguments.of(null, false),
                Arguments.of("", false)
        );
    }
}
