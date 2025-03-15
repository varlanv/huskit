package io.huskit.common;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VolatileTest implements UnitTest {

    @Test
    @DisplayName("of no args returns empty volatile")
    void of_no_args_returns_empty_volatile() {
        var subject = Volatile.of();

        assertThat(subject.isPresent()).isFalse();
        assertThat(subject).isInstanceOf(DfVolatile.class);
    }

    @Test
    @DisplayName("of with arg returns volatile with value")
    void of_with_arg_returns_volatile_with_value() {
        var value = "value";
        var subject = Volatile.of(value);

        assertThat(subject.require()).isEqualTo(value);
        assertThat(subject).isInstanceOf(DfVolatile.class);
    }

    @Test
    @DisplayName("of another volatile returns volatile with value")
    void of_another_volatile_returns_volatile_with_value() {
        var value = "value";
        var other = Volatile.of(value);
        var subject = Volatile.of(other);

        assertThat(subject.require()).isEqualTo(value);
        assertThat(subject).isInstanceOf(DfVolatile.class);
    }
}
