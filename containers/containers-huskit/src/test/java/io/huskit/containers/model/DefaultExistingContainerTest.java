package io.huskit.containers.model;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultExistingContainerTest implements UnitTest {

    @Test
    @DisplayName("`isExpired` if container is expired, should return true")
    void isexpired_if_container_is_expired_should_return_true() {
        var nowMinus10Sec = System.currentTimeMillis() - Duration.ofSeconds(10).toMillis();
        var subject = buildSubject(nowMinus10Sec);

        var actual = subject.isExpired(Duration.ofSeconds(5));

        assertThat(actual).isTrue();
    }

    @Test
    @DisplayName("`isExpired` if container not expired, should return false")
    void isexpired_if_container_not_expired_should_return_false() {
        var nowMinus10Sec = System.currentTimeMillis() - Duration.ofSeconds(10).toMillis();
        var subject = buildSubject(nowMinus10Sec);

        var actual = subject.isExpired(Duration.ofSeconds(15));

        assertThat(actual).isFalse();
    }

    @Test
    @DisplayName("`isExpired` if cleanupAfter is 0, should return false")
    void isexpired_if_cleanupafter_is_0_should_return_false() {
        var subject = buildSubject(System.currentTimeMillis());

        assertThat(subject.isExpired(Duration.ofSeconds(0))).isFalse();
    }

    private DefaultExistingContainer buildSubject(long createdAt) {
        return new DefaultExistingContainer("id", "containerId", createdAt, Map.of());
    }
}
