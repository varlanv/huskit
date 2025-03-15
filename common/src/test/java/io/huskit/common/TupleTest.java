package io.huskit.common;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TupleTest implements UnitTest {

    @Test
    @DisplayName("'toList' returns list")
    void tolist_returns_list() {
        var subject = Tuple.of("left", "right");

        var result = subject.toList();

        assertEquals(2, result.size());
        assertEquals("left", result.get(0));
        assertEquals("right", result.get(1));
    }

    @Test
    @DisplayName("of when left null throws exception")
    void of_when_left_null_throws_exception() {
        var right = "right";

        assertThrows(NullPointerException.class, () -> Tuple.of(null, right));
    }

    @Test
    @DisplayName("of when right null throws exception")
    void of_when_right_null_throws_exception() {
        var left = "left";

        assertThrows(NullPointerException.class, () -> Tuple.of(left, null));
    }

    @Test
    @DisplayName("of when both null throws exception")
    void of_when_both_null_throws_exception() {
        assertThrows(NullPointerException.class, () -> Tuple.of(null, null));
    }
}
