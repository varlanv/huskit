package io.huskit.common;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NothingTest implements UnitTest {

    @Test
    @DisplayName("always returns same instance")
    void always_returns_same_instance() {
        var instance = Nothing.instance();
        assertThat(instance).isSameAs(Nothing.instance());
    }
}
