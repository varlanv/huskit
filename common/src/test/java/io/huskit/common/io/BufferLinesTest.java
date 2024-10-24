package io.huskit.common.io;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class BufferLinesTest implements UnitTest {

    @Test
    void when_new_line_not_found_within_nest_limit__throw_exception() {
        var nestLimit = 1000;
        var subject = new BufferLines(() -> "asd".getBytes(StandardCharsets.UTF_8), nestLimit);
        assertThatThrownBy(subject::next)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Couldn't find new line after %s reads", nestLimit);
    }

    @Test
    void when_input_contains_only_crlf__then_return_empty_line() {
        var subject = new BufferLines(() -> "\r\n".getBytes(StandardCharsets.UTF_8));

        var actual = subject.next();

        assertThat(actual.value()).isEqualTo("");
    }

    @Test
    void when_input_contains_only_crlf_split_between_two_inputs__then_return_empty_line() {
        var counter = new AtomicInteger();
        var subject = new BufferLines(() -> {
            if (counter.getAndIncrement() == 0) {
                return "\r".getBytes(StandardCharsets.UTF_8);
            } else {
                return "\n".getBytes(StandardCharsets.UTF_8);
            }
        });

        var actual = subject.next();

        assertThat(actual.value()).isEqualTo("");
    }

    @Test
    void when_input_contains_only_1_letter_and_then_crlf__then_return_1_letter() {
        var subject = new BufferLines(() -> "q\r\n".getBytes(StandardCharsets.UTF_8));

        var actual = subject.next();

        assertThat(actual.value()).isEqualTo("q");
    }

    @Test
    void when_read_many_lines__should_work_correctly() {
        var linesCount = 20;
        var wordBase = "qwerty";
        var bytes = IntStream.rangeClosed(0, linesCount)
                .mapToObj(i -> "qwerty" + i + "\r\n")
                .collect(Collectors.joining())
                .getBytes(StandardCharsets.UTF_8);

        var subject = new BufferLines(() -> bytes);

        for (var i = 0; i < linesCount; i++) {
            assertThat(subject.next().value()).isEqualTo(wordBase + i);
        }
    }

    @Test
    void when_input_contains_two_lines_in_same_input__then_should_read_it_correctly() {
        var subject = new BufferLines(() -> "qwe\r\nrty\r\n".getBytes(StandardCharsets.UTF_8));

        assertThat(subject.next().value()).isEqualTo("qwe");
        assertThat(subject.next().value()).isEqualTo("rty");
    }

    @Test
    void when_input_contains_two_lines_in_same_input_and_second_line_is_empty__then_should_read_it_correctly() {
        var subject = new BufferLines(() -> "qwe\r\n\r\n".getBytes(StandardCharsets.UTF_8));

        assertThat(subject.next().value()).isEqualTo("qwe");
        assertThat(subject.next().value()).isEqualTo("");
        assertThat(subject.next().value()).isEqualTo("qwe");
        assertThat(subject.next().value()).isEqualTo("");
    }

    @Test
    void when_input_contains_two_lines_in_first_input_and_one_empty_line_split_be_two_next_inputs__then_should_read_it_correctly() {
        var counter = new AtomicInteger();
        var subject = new BufferLines(() -> {
            if (counter.incrementAndGet() == 1) {
                return "qwe\r\n\r\n".getBytes(StandardCharsets.UTF_8);
            } else if (counter.get() == 2) {
                return "\r".getBytes(StandardCharsets.UTF_8);
            } else {
                return "\n".getBytes(StandardCharsets.UTF_8);
            }
        });

        assertThat(subject.next().value()).isEqualTo("qwe");
        assertThat(subject.next().value()).isEqualTo("");
        assertThat(subject.next().value()).isEqualTo("");
    }

    @Test
    void when_input_contains_4_letters_and_then_crlf__then_return_1_letter() {
        var subject = new BufferLines(() -> "asdf\r\n".getBytes(StandardCharsets.UTF_8));

        var actual = subject.next();

        assertThat(actual.value()).isEqualTo("asdf");
    }

    @Test
    void when_input_contains_4_letters_and_then_crlf_inputs__then_return_1_letter() {
        var counter = new AtomicInteger();
        var subject = new BufferLines(() -> {
            if (counter.getAndIncrement() == 0) {
                return "asdf".getBytes(StandardCharsets.UTF_8);
            } else {
                return "\r\n".getBytes(StandardCharsets.UTF_8);
            }
        });

        var actual = subject.next();

        assertThat(actual.value()).isEqualTo("asdf");
    }

    @Test
    void when_input_contains_2_crlf_split_between_4_inputs__then_return_2_empty_lines() {
        var counter = new AtomicInteger();
        var subject = new BufferLines(() -> {
            if (counter.getAndIncrement() == 0) {
                return "\r".getBytes(StandardCharsets.UTF_8);
            } else if (counter.get() == 2) {
                return "\n".getBytes(StandardCharsets.UTF_8);
            } else if (counter.get() == 3) {
                return "\r".getBytes(StandardCharsets.UTF_8);
            } else if (counter.get() == 4) {
                return "\n".getBytes(StandardCharsets.UTF_8);
            } else {
                return "\r".getBytes(StandardCharsets.UTF_8);
            }
        });

        assertThat(subject.next().value()).isEqualTo("");
        assertThat(subject.next().value()).isEqualTo("");
        assertThatThrownBy(subject::next)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void when_input_contains_only_1_letter_and_then_crlf_split_between_two_inputs__then_return_1_letter() {
        var counter = new AtomicInteger();
        var subject = new BufferLines(() -> {
            if (counter.getAndIncrement() == 0) {
                return "q".getBytes(StandardCharsets.UTF_8);
            } else if (counter.get() == 2) {
                return "\r".getBytes(StandardCharsets.UTF_8);
            } else {
                return "\n".getBytes(StandardCharsets.UTF_8);
            }
        });

        var actual = subject.next();

        assertThat(actual.value()).isEqualTo("q");
    }

    @Test
    void when_input_contains_only_asdf_letter_and_then_crlf_split_between_two_inputs__then_return_1_letter() {
        var counter = new AtomicInteger();
        var subject = new BufferLines(() -> {
            if (counter.getAndIncrement() == 0) {
                return "asdf".getBytes(StandardCharsets.UTF_8);
            } else if (counter.get() == 2) {
                return "\r".getBytes(StandardCharsets.UTF_8);
            } else {
                return "\n".getBytes(StandardCharsets.UTF_8);
            }
        });

        var actual = subject.next();

        assertThat(actual.value()).isEqualTo("asdf");
    }

    @Test
    void when_new_line_found_on_first_read__return_line() {
        var subject = new BufferLines(() -> "qwe\r\n".getBytes(StandardCharsets.UTF_8));

        var actual = subject.next();

        assertThat(actual.value()).isEqualTo("qwe");
    }

    @Test
    void when_new_line_found_on_second_read__return_line() {
        var counter = new AtomicInteger();
        var subject = new BufferLines(() -> {
            if (counter.getAndIncrement() == 0) {
                return "qwe".getBytes(StandardCharsets.UTF_8);
            } else {
                return "rty\r\n".getBytes(StandardCharsets.UTF_8);
            }
        });

        var actual = subject.next();

        assertThat(actual.value()).isEqualTo("qwerty");
    }

    @Test
    void when_new_line_found_on_third_read__return_line() {
        var counter = new AtomicInteger();
        var subject = new BufferLines(() -> {
            if (counter.getAndIncrement() == 0) {
                return "qwe".getBytes(StandardCharsets.UTF_8);
            } else if (counter.get() == 2) {
                return "rty".getBytes(StandardCharsets.UTF_8);
            } else {
                return "uio\r\n".getBytes(StandardCharsets.UTF_8);
            }
        });

        var actual = subject.next();

        assertThat(actual.value()).isEqualTo("qwertyuio");
    }

    @Test
    void should_be_able_to_find_three_new_lines() {
        var counter = new AtomicInteger();
        var subject = new BufferLines(() -> {
            if (counter.getAndIncrement() == 0) {
                return "qwe\r\n".getBytes(StandardCharsets.UTF_8);
            } else if (counter.get() == 2) {
                return "rty\r\n".getBytes(StandardCharsets.UTF_8);
            } else {
                return "uio\r\n".getBytes(StandardCharsets.UTF_8);
            }
        });


        assertThat(subject.next().value()).isEqualTo("qwe");
        assertThat(subject.next().value()).isEqualTo("rty");
        assertThat(subject.next().value()).isEqualTo("uio");
    }

    @Test
    void when_new_line_cr_lf_is_spit_between_two_buffers__return_line() {
        var counter = new AtomicInteger();
        var subject = new BufferLines(() -> {
            if (counter.getAndIncrement() == 0) {
                return "qwe\r".getBytes(StandardCharsets.UTF_8);
            } else {
                return "\nrty".getBytes(StandardCharsets.UTF_8);
            }
        });

        var actual = subject.next();

        assertThat(actual.value()).isEqualTo("qwe");
    }

    @Test
    void when_big_line__should_split_correctly() {
        var linesCount = 150;
        var linesSize = 1000;
        var array = IntStream.range(0, linesCount)
                .mapToObj(i -> String.valueOf(i).repeat(linesSize) + "\r\n")
                .map(line -> line.getBytes(StandardCharsets.UTF_8))
                .reduce(new byte[0], (acc, line) -> {
                    var newBytes = new byte[acc.length + line.length];
                    System.arraycopy(acc, 0, newBytes, 0, acc.length);
                    System.arraycopy(line, 0, newBytes, acc.length, line.length);
                    return newBytes;
                });

        var subject = new BufferLines(() -> array);

        for (var i = 0; i < linesCount; i++) {
            var actual = subject.next();
            var expected = String.valueOf(i).repeat(linesSize);
            assertThat(actual.value()).isEqualTo(expected);
        }
    }

    @Test
    @Disabled
    void performance_test_big_lines() throws Exception {
        var linesCount = 150;
        var linesSize = 1000;
        compareWithBufferedReader(
                IntStream.range(0, linesCount)
                        .mapToObj(i -> String.valueOf(i).repeat(linesSize))
                        .toArray(String[]::new)
        );
    }

    @Test
    @Disabled
    void performance_test_avg_lines() throws Exception {
        var linesCount = 40;
        var linesSize = 30;
        compareWithBufferedReader(
                IntStream.range(0, linesCount)
                        .mapToObj(i -> String.valueOf(i).repeat(linesSize))
                        .toArray(String[]::new)
        );
    }

    private void compareWithBufferedReader(String... lines) throws Exception {
        var linesList = Arrays.stream(lines)
                .map(line -> line + "\r\n")
                .collect(Collectors.toList());
        var bytes = linesList.stream()
                .map(line -> line.getBytes(StandardCharsets.UTF_8))
                .reduce(new byte[0], (acc, line) -> {
                    var newBytes = new byte[acc.length + line.length];
                    System.arraycopy(acc, 0, newBytes, 0, acc.length);
                    System.arraycopy(line, 0, newBytes, acc.length, line.length);
                    return newBytes;
                });

        var iterations = 300;
//        var newLineCounter = new NewLineCounter(bytes);
        microBenchmark(iterations, "LineReader", () -> {
            var lineReader = new BufferLines(() -> bytes);
            var iterationsCount = new AtomicInteger();
            for (var i = 0; i < lines.length; i++) {
                assertThat(lineReader.next().value()).isNotEmpty();
                iterationsCount.incrementAndGet();
            }
            return iterationsCount;
        });

        microBenchmark(iterations, "BufferedReader", () -> {
            var bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes)));
            var iterationsCount = new AtomicInteger();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                assertThat(line).isNotEmpty();
                iterationsCount.incrementAndGet();
            }
            return iterationsCount.get();
        });

        var string = new String(bytes, StandardCharsets.UTF_8);
        microBenchmark(iterations, "String.lines()", () -> {
            var iterationsCount = new AtomicInteger();
            string.lines().forEach(line -> {
                assertThat(line).isNotEmpty();
                iterationsCount.incrementAndGet();

            });
            assertThat(iterationsCount.get()).isEqualTo(lines.length);
            return iterationsCount.get();
        });
    }
}
