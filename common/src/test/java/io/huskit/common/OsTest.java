package io.huskit.common;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OsTest implements UnitTest {

    @Test
    @DisplayName("if one current then other is not current")
    void if_one_current_then_other_is_not_current() {
        if (Os.current() == Os.LINUX) {
            assertThat(Os.WINDOWS.isCurrent()).isFalse();
            assertThat(Os.MAC.isCurrent()).isFalse();
        } else if (Os.current() == Os.WINDOWS) {
            assertThat(Os.LINUX.isCurrent()).isFalse();
            assertThat(Os.MAC.isCurrent()).isFalse();
        } else if (Os.current() == Os.MAC) {
            assertThat(Os.WINDOWS.isCurrent()).isFalse();
            assertThat(Os.LINUX.isCurrent()).isFalse();
        }
    }
}
