package io.huskit.common;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptTest implements UnitTest {

    String value = "value";

    @Nested
    class SomeTest {

        @Test
        @DisplayName("of returns some instance")
        void of_returns_some_instance() {
            assertThat(Opt.of(value)).isInstanceOf(Some.class);
        }

        @Test
        @DisplayName("of throws exception when value is null")
        void of_throws_exception_when_value_is_null() {
            assertThatThrownBy(() -> Opt.of(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("require returns value")
        void require_returns_value() {
            assertThat(new Some<>(value).require()).isEqualTo(value);
        }

        @Test
        @DisplayName("'isPresent' returns true")
        void ispresent_returns_true() {
            assertThat(new Some<>(value).isPresent()).isTrue();
        }

        @Test
        @DisplayName("'isEmpty' returns false")
        void isempty_returns_false() {
            assertThat(new Some<>(value).isEmpty()).isFalse();
        }

        @Test
        @DisplayName("'ifPresent' executes consumer")
        void ifpresent_executes_consumer() {
            var subject = new Some<>(value);
            var result = new StringBuilder();

            subject.ifPresent(result::append);

            assertThat(result.toString()).isEqualTo(value);
        }
    }

    @Nested
    class NoneTest {

        @Test
        @DisplayName("require throws exception")
        void require_throws_exception() {
            assertThatThrownBy(() -> None.instance().require())
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("'isPresent' returns false")
        void ispresent_returns_false() {
            assertThat(None.instance().isPresent()).isFalse();
        }

        @Test
        @DisplayName("'isEmpty' returns true")
        void isempty_returns_true() {
            assertThat(None.instance().isEmpty()).isTrue();
        }

        @Test
        @DisplayName("'ifPresent' does nothing")
        void ifpresent_does_nothing() {
            var result = new StringBuilder();

            None.instance().ifPresent(result::append);

            assertThat(result.toString()).isEmpty();
        }

        @Test
        @DisplayName("empty returns none instance")
        void empty_returns_none_instance() {
            assertThat(Opt.empty()).isInstanceOf(None.class);
        }
    }
}
