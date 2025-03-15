package io.huskit.common.io;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NewLineCounterTest implements UnitTest {

    @Test
    @Disabled
    @DisplayName("long input performance check")
    void long_input_performance_check() {
        var iterations = 200;
        var longInput = "qwerty\r\n".repeat(100000).getBytes(StandardCharsets.UTF_8);
        microBenchmark(iterations, "Long input", () -> new NewLineCounter(longInput).positions());

        var shortInput = "qwertyasdfghzxcvbn: qweqwrqwras\r\n".repeat(15).getBytes(StandardCharsets.UTF_8);
        microBenchmark(iterations, "Short input", () -> new NewLineCounter(shortInput).positions());
    }

    @Test
    @DisplayName("positions varied length line")
    void positions_varied_length_line() {
        verify(
            "\r\na\r\nbb\r\n\r\nccc\r\ndddd\r\n",
            new int[]{0, 3, 7, 9, 14, 20}
        );
    }

    @ParameterizedTest
    @MethodSource("positionShouldReturnPositionsOfNewLines")
    @DisplayName("positions should return positions of new lines")
    void positions_should_return_positions_of_new_lines(String input, int[] expected) {
        verify(input, expected);
    }

    @Test
    @DisplayName("positions qwerty repeated 50")
    void positions_qwerty_repeated_50() {
        verify(
            "qwerty\r\n".repeat(50),
            IntStream.range(0, 50).map(i -> i * 8 + 6).toArray()
        );
    }

    @Test
    @DisplayName("positions qwerty repeated 3")
    void positions_qwerty_repeated_3() {
        verify(
            "qwerty\r\nqwerty\r\nqwerty\r\n",
            new int[]{6, 14, 22}
        );
    }

    @Test
    @DisplayName("positions qwerty repeated 3 start at 2")
    void positions_qwerty_repeated_3_start_at_2() {
        verify(
            "qwerty\r\nqwerty\r\nqwerty\r\n",
            2,
            new int[]{6, 14, 22}
        );
    }

    @Test
    @DisplayName("positions qwerty repeated 3 start at 7")
    void positions_qwerty_repeated_3_start_at_7() {
        verify(
            "qwerty\r\nqwerty\r\nqwerty\r\n",
            7,
            new int[]{14, 22}
        );
    }

    @Test
    @DisplayName("positions qwerty repeated 3 start at 15")
    void positions_qwerty_repeated_3_start_at_15() {
        verify(
            "qwerty\r\nqwerty\r\nqwerty\r\n",
            15,
            new int[]{22}
        );
    }

    @Test
    @DisplayName("positions qwerty repeated 3 start at 6")
    void positions_qwerty_repeated_3_start_at_6() {
        verify(
            "qwerty\r\nqwerty\r\nqwerty\r\n",
            6,
            new int[]{6, 14, 22}
        );
    }

    @Test
    @DisplayName("positions qwerty repeated 3 with new line at start")
    void positions_qwerty_repeated_3_with_new_line_at_start() {
        verify(
            "\r\nqwerty\r\nqwerty\r\nqwerty\r\n",
            new int[]{0, 8, 16, 24}
        );
    }

    @Test
    @DisplayName("positions qwertyu repeated 50")
    void positions_qwertyu_repeated_50() {
        var input = "qwertyu\r\n".repeat(50);
        var expected = IntStream.range(0, 50)
            .map(i -> i * 9 + 7)
            .toArray();

        verify(
            input,
            expected
        );
    }

    @Test
    @DisplayName("positions qwertyu repeated 3")
    void positions_qwertyu_repeated_3() {
        verify(
            "qwertyu\r\nqwertyu\r\nqwertyu\r\n",
            new int[]{7, 16, 25}
        );
    }

    @Test
    @DisplayName("positions qwertyu repeated 3 with new line at start")
    void positions_qwertyu_repeated_3_with_new_line_at_start() {
        verify(
            "\r\nqwertyu\r\nqwertyu\r\nqwertyu\r\n",
            new int[]{0, 9, 18, 27}
        );
    }

    private Stream<Arguments> positionShouldReturnPositionsOfNewLines() {
        return Stream.of(
            Arguments.of("a\r\nb\r\nc\r\nd", new int[]{1, 4, 7}),
            Arguments.of("\r\n\r\n", new int[]{0, 2}),
            Arguments.of("", new int[]{}),
            Arguments.of("\r\n", new int[]{0}),
            Arguments.of("\n", new int[]{}),
            Arguments.of("a\r\n\r\nb\r\nc", new int[]{1, 3, 6}),
            Arguments.of("a\r\n\r\nb\r\nc\r\n", new int[]{1, 3, 6, 9})
        );
    }

    private void verify(String input, int[] expected) {
        verify(input, 0, expected);
    }

    private void verify(String input, Integer initialPosition, int[] expected) {
        var array = input.getBytes(StandardCharsets.UTF_8);
        var subject = new NewLineCounter(array, initialPosition);
        var res = subject.positions();
        assertThat(res).satisfies(
            ignore -> assertThat(res.limit()).isEqualTo(expected.length),
            ignore -> assertThat(Arrays.copyOf(res.array(), res.limit())).isEqualTo(expected)
        );

        for (var pos : expected) {
            assertThat(subject.nextPosition()).isEqualTo(pos);
        }
        assertThat(subject.nextPosition()).isEqualTo(-1);
    }
}
