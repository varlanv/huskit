package io.huskit.common.io;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoopInputStreamTest implements UnitTest {

    @Test
    @DisplayName("read should read bytes in loop")
    void read_should_read_bytes_in_loop() {
        var subject = new LoopInputStream(() -> new ByteArrayInputStream(new byte[]{1, 2, 3}));

        assertThat(subject.read()).isEqualTo(1);
        assertThat(subject.read()).isEqualTo(2);
        assertThat(subject.read()).isEqualTo(3);
        assertThat(subject.read()).isEqualTo(1);
        assertThat(subject.read()).isEqualTo(2);
        assertThat(subject.read()).isEqualTo(3);
        assertThat(subject.read()).isEqualTo(1);
    }

    @Test
    @DisplayName("read should call supplier only when end of stream")
    void read_should_call_supplier_only_when_end_of_stream() {
        var counter = new AtomicInteger();
        var subject = new LoopInputStream(() -> {
            counter.incrementAndGet();
            return new ByteArrayInputStream(new byte[]{1, 2, 3});
        });
        assertThat(counter.get()).isEqualTo(1);

        subject.read();
        assertThat(counter.get()).isEqualTo(1);
        subject.read();
        assertThat(counter.get()).isEqualTo(1);
        subject.read();
        assertThat(counter.get()).isEqualTo(1);
        subject.read();
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("close should close delegate")
    void close_should_close_delegate(@TempDir Path dir) throws Exception {
        var file = Files.createFile(dir.resolve("test")).toFile();
        var fileInputStream = new FileInputStream(file);
        var subject = new LoopInputStream(() -> fileInputStream);

        subject.close();

        assertThatThrownBy(fileInputStream::read)
                .isInstanceOf(IOException.class)
                .message()
                .containsIgnoringCase("Stream closed");
    }
}
