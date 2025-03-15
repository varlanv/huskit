package io.huskit.common.port;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResolvedPortTest implements UnitTest {

    Integer hostValue = 11119;
    Integer containerValue = 2;
    Boolean isFixed = true;
    ResolvedPort subject = new ResolvedPort(hostValue, containerValue, isFixed);

    @Test
    @DisplayName("`hostValue` should return host port")
    void hostvalue_should_return_host_port() {
        assertThat(subject.hostValue()).isEqualTo(hostValue);
    }

    @Test
    @DisplayName("`containerValue` should return container port")
    void containervalue_should_return_container_port() {
        assertThat(subject.containerValue()).contains(containerValue);
    }

    @Test
    @DisplayName("`isFixed` should return true")
    void isfixed_should_return_true() {
        assertThat(subject.isFixed()).isTrue();
    }
}
