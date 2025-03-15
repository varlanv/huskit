package io.huskit.containers.http;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushJsonArrayTest implements UnitTest {

    @Test
    @DisplayName("push when body is present and empty array should return empty")
    void push_when_body_is_present_and_empty_array_should_return_empty() {
        var subject = new PushJsonArray();

        var maybeActual = subject.push(ByteBuffer.wrap("[]".getBytes()));

        assertThat(maybeActual)
            .hasValueSatisfying(
                actual -> assertThat(actual).isEmpty()
            );
    }

    @Test
    @DisplayName("value when not pushed is empty")
    void value_when_not_pushed_is_empty() {
        var subject = new PushJsonArray();

        assertThat(subject.value()).isEmpty();
    }

    @Test
    @DisplayName("push when body is present and array with one element should return array with one element")
    void push_when_body_is_present_and_array_with_one_element_should_return_array_with_one_element() {
        var subject = new PushJsonArray();

        var maybeActual = subject.push(ByteBuffer.wrap("[{\"key\":\"value\"}]".getBytes()));

        assertThat(maybeActual)
            .hasValueSatisfying(
                actual -> assertThat(actual)
                    .hasSize(1)
                    .first().satisfies(
                        element -> assertThat(element).containsEntry("key", "value")
                    )
            );
    }

    @Test
    @DisplayName("push when body is invalid json should throw exception")
    void push_when_body_is_invalid_json_should_throw_exception() {
        var subject = new PushJsonArray();

        assertThatThrownBy(
            () -> subject.push(ByteBuffer.wrap("invalid json".getBytes()))
        ).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("value when pushed should return pushed value")
    void value_when_pushed_should_return_pushed_value() {
        var subject = new PushJsonArray();

        subject.push(ByteBuffer.wrap("[{\"key\":\"value\"}]".getBytes()));

        assertThat(subject.value())
            .hasValueSatisfying(
                actual -> assertThat(actual)
                    .hasSize(1)
                    .first().satisfies(
                        element -> assertThat(element).containsEntry("key", "value")
                    )
            );
    }

    @Test
    @DisplayName("push when result was already set should throw exception")
    void push_when_result_was_already_set_should_throw_exception() {
        var subject = new PushJsonArray();

        subject.push(ByteBuffer.wrap("[]".getBytes()));

        assertThatThrownBy(
            () -> subject.push(ByteBuffer.wrap("[]".getBytes()))
        ).isInstanceOf(IllegalStateException.class);
    }
}
