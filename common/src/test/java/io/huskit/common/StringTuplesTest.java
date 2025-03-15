package io.huskit.common;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringTuplesTest implements UnitTest {

    @Test
    @DisplayName("'toList' empty")
    void tolist_empty() {
        var subject = new StringTuples();

        var actual = subject.toList();

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("'toList' varargs 1")
    void tolist_varargs_1() {
        var subject = new StringTuples("a");

        var actual = subject.toList();

        assertThat(actual).containsExactly("a");
    }

    @Test
    @DisplayName("'toList' varargs 2")
    void tolist_varargs_2() {
        var subject = new StringTuples("a", "b");

        var actual = subject.toList();

        assertThat(actual).containsExactly("a", "b");
    }

    @Test
    @DisplayName("'toList' varargs 3")
    void tolist_varargs_3() {
        var subject = new StringTuples("a", "b", "c");

        var actual = subject.toList();

        assertThat(actual).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("'toList' varargs 11")
    void tolist_varargs_11() {
        var subject = new StringTuples("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");

        var actual = subject.toList();

        assertThat(actual).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");
    }

    @Test
    @DisplayName("'toList' collection 1")
    void tolist_collection_1() {
        var subject = new StringTuples(List.of("a"));

        var actual = subject.toList();

        assertThat(actual).containsExactly("a");
    }

    @Test
    @DisplayName("'toList' collection 2")
    void tolist_collection_2() {
        var subject = new StringTuples(List.of("a", "b"));

        var actual = subject.toList();

        assertThat(actual).containsExactly("a", "b");
    }

    @Test
    @DisplayName("'toList' collection 3")
    void tolist_collection_3() {
        var subject = new StringTuples(List.of("a", "b", "c"));

        var actual = subject.toList();

        assertThat(actual).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("'toList' when adding existing tuple then no duplicates")
    void tolist_when_adding_existing_tuple_then_no_duplicates() {
        var subject = new StringTuples("a", "b");
        subject.add("a", "b");

        var actual = subject.toList();

        assertThat(actual).containsExactly("a", "b");
    }

    @Test
    @DisplayName("'toList' when adding key that exists in tuple pair then adds key")
    void tolist_when_adding_key_that_exists_in_tuple_pair_then_adds_key() {
        var subject = new StringTuples("a", "b");
        subject.add("a");

        var actual = subject.toList();

        assertThat(actual).containsExactly("a", "b", "a");
    }

    @Test
    @DisplayName("'toList' when adding key that exists in tuple pair then adds value")
    void tolist_when_adding_key_that_exists_in_tuple_pair_then_adds_value() {
        var subject = new StringTuples("a", "b", "c");
        subject.add("c");

        var actual = subject.toList();

        assertThat(actual).containsExactly("a", "b", "c");
    }
}
