package io.huskit.common.port;

import io.huskit.common.Volatile;
import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.BindException;
import java.net.SocketException;

import static org.assertj.core.api.Assertions.assertThat;

class FixedContainerPortTest implements UnitTest {

    Integer hostPort = 11119;
    Integer containerPort = 2;

    @Test
    @DisplayName("`containerValue` should return container port")
    void containervalue_should_return_container_port() {
        assertThat(new FixedContainerPort(hostPort, containerPort).containerValue()).contains(containerPort);
    }

    @Test
    @DisplayName("`isFixed` should return true")
    void isfixed_should_return_true() {
        assertThat(new FixedContainerPort(hostPort, containerPort).isFixed()).isTrue();
    }

    @Test
    @DisplayName("`hostValue` should return value or throw bind exception if port is already in use")
    void hostvalue_should_return_value_or_throw_bind_exception_if_port_is_already_in_use() {
        // this test check two cases at once to avoid flakiness
        var exceptionRef = Volatile.<Throwable>of();
        parallel(5, () -> {
            try {
                assertThat(new FixedContainerPort(hostPort, containerPort).hostValue()).isEqualTo(hostPort);
            } catch (Throwable e) {
                exceptionRef.set(e);
            }
        });
        exceptionRef.maybe().ifPresent(exception -> assertThat(exception)
                .isInstanceOfAny(BindException.class, SocketException.class));
    }
}
