package io.huskit;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringLinesTest implements UnitTest {

    @Test
    @DisplayName("check how string lines work")
    void check_how_string_lines_work() {
        assertThat("".lines()).isEmpty();
        assertThat("\n".lines()).containsExactly("");
        assertThat("a".lines()).containsExactly("a");
        assertThat("a\nb".lines()).containsExactly("a", "b");
        assertThat("\r".lines()).containsExactly("");
        assertThat("\r\n".lines()).containsExactly("");
        assertThat("\r\n\r\n".lines()).containsExactly("", "");
        assertThat("\r\r".lines()).containsExactly("", "");
        assertThat("йц\r\nb".lines()).containsExactly("йц", "b");
        assertThat("a\n".lines()).containsExactly("a");
        assertThat("a\r\nb\nq".lines()).containsExactly("a", "b", "q");
        assertThat("a\r\nb\\nq".lines()).containsExactly("a", "b\\nq");
        assertThat("\\r".lines()).containsExactly("\\r");
        assertThat("a\\r\nq".lines()).containsExactly("a\\r", "q");
    }
}
