package io.huskit.common;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SneakyTest implements UnitTest {

    @Test
    @DisplayName("rethrow when throwable throws throwable")
    void rethrow_when_throwable_throws_throwable() {
        var throwable = new Throwable();

        assertThatThrownBy(() -> Sneaky.rethrow(throwable))
                .isSameAs(throwable);
    }

    @Test
    @DisplayName("rethrow when null throws null pointer exception")
    void rethrow_when_null_throws_null_pointer_exception() {
        assertThatThrownBy(() -> Sneaky.rethrow(null))
                .isInstanceOf(NullPointerException.class);
    }
}
