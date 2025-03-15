package io.huskit.containers.http;

import io.huskit.gradle.commontest.Args;
import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class PushHeadTest implements UnitTest {

    @Test
    @Args.HttpHeaders
    @DisplayName("push when headers are present should return head")
    void push_when_headers_are_present_should_return_head(String headers, Consumer<Map<String, String>> headersMatcher) {
        var subject = new PushHead();
        var byteBuffer = ByteBuffer.wrap(headers.getBytes(StandardCharsets.UTF_8));

        var maybeActual = subject.push(byteBuffer);

        assertThat(maybeActual)
            .isPresent()
            .hasValueSatisfying(
                actual -> {
                    headersMatcher.accept(actual.headers());
                    assertThat(actual.status()).isEqualTo(200);
                    assertThat(actual.isChunked()).isFalse();
                    assertThat(actual.isMultiplexedStream()).isFalse();
                    assertThat(actual.isChunked()).isFalse();
                    assertThat(actual.isMultiplexedStream()).isFalse();
                    assertThat(byteBuffer.position()).isEqualTo(headers.length());
                }
            );
    }
}
