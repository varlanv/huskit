package io.huskit.common.io;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ByteBufferInputStreamTest implements UnitTest {

    @Test
    @DisplayName("when null buffer throws")
    void when_null_buffer_throws() {
        assertThatThrownBy(() -> new ByteBufferInputStream(null)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("read should return data")
    void read_should_return_data() {
        var buffer = ByteBuffer.wrap("Hello".getBytes(StandardCharsets.UTF_8));
        var subject = new ByteBufferInputStream(buffer);

        assertThat(subject.read()).isEqualTo('H');
        assertThat(subject.read()).isEqualTo('e');
        assertThat(subject.read()).isEqualTo('l');
        assertThat(subject.read()).isEqualTo('l');
        assertThat(subject.read()).isEqualTo('o');
        assertThat(subject.read()).isEqualTo(-1);
        assertThat(subject.read()).isEqualTo(-1);
    }

    @Test
    @DisplayName("read when empty buffer should return minus one")
    void read_when_empty_buffer_should_return_minus_one() {
        var buffer = ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8));
        var subject = new ByteBufferInputStream(buffer);

        assertThat(subject.read()).isEqualTo(-1);
    }

    @Test
    @DisplayName("read to byte array should return data")
    void read_to_byte_array_should_return_data() {
        var buffer = ByteBuffer.wrap("Hello".getBytes(StandardCharsets.UTF_8));
        var subject = new ByteBufferInputStream(buffer);
        var bytes = new byte[5];

        assertThat(subject.read(bytes, 0, 5)).isEqualTo(5);
        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("Hello");
    }

    @Test
    @DisplayName("read to byte array when not enough space should return data")
    void read_to_byte_array_when_not_enough_space_should_return_data() {
        var buffer = ByteBuffer.wrap("Hello".getBytes(StandardCharsets.UTF_8));
        var subject = new ByteBufferInputStream(buffer);
        var bytes = new byte[3];

        assertThat(subject.read(bytes, 0, 3)).isEqualTo(3);
        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("Hel");
    }

    @Test
    @DisplayName("read to byte array when not enough data should return data")
    void read_to_byte_array_when_not_enough_data_should_return_data() {
        var buffer = ByteBuffer.wrap("Hello".getBytes(StandardCharsets.UTF_8));
        var subject = new ByteBufferInputStream(buffer);
        var bytes = new byte[10];

        assertThat(subject.read(bytes, 0, 10)).isEqualTo(5);
        assertThat(new String(bytes, 0, 5, StandardCharsets.UTF_8)).isEqualTo("Hello");
    }

    @Test
    @DisplayName("read to byte array when empty buffer should return minus one")
    void read_to_byte_array_when_empty_buffer_should_return_minus_one() {
        var buffer = ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8));
        var subject = new ByteBufferInputStream(buffer);
        var bytes = new byte[5];

        assertThat(subject.read(bytes, 0, 5)).isEqualTo(-1);
    }
}
