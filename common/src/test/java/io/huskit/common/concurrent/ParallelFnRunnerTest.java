package io.huskit.common.concurrent;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParallelFnRunnerTest implements UnitTest {

    @Test
    @DisplayName("'doParallel' when empty returns empty")
    void doparallel_when_empty_returns_empty() {
        var subject = new ParallelFnRunner<String, String>(List.of());
        Function<String, String> function = a -> {
            throw new RuntimeException("Should not be called");
        };

        var result = subject.doParallel(function);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("'doParallel' when one element should run in same thread")
    void doparallel_when_one_element_should_run_in_same_thread() {
        var subject = new ParallelFnRunner<String, String>(List.of(() -> "value"));
        var fnThread = new AtomicReference<Thread>();
        Function<String, String> function = a -> {
            fnThread.set(Thread.currentThread());
            return a;
        };

        var result = subject.doParallel(function);

        assertThat(result).containsExactly("value");
        assertThat(fnThread.get()).isEqualTo(Thread.currentThread());
    }

    @RepeatedTest(5)
    @DisplayName("'doParallel' when two element should return ordered results")
    void doparallel_when_two_element_should_return_ordered_results() {
        var subject = new ParallelFnRunner<String, String>(
                List.of(
                        () -> "value1",
                        () -> "value2"
                ));
        Function<String, String> function = a -> a;

        var result = subject.doParallel(function);

        assertThat(result).containsExactly("value1", "value2");
    }

    @Test
    @DisplayName("'doParallel' two elements with consumer should return ordered results")
    void doparallel_two_elements_with_consumer_should_return_ordered_results() {
        var subject = new ParallelFnRunner<String, String>(
                List.of(
                        () -> "value1",
                        () -> "value2"
                ));
        List<String> result = new CopyOnWriteArrayList<>();
        Consumer<String> consumer = result::add;

        subject.doParallel(consumer);

        assertThat(result).containsExactlyInAnyOrder("value1", "value2");
    }

    @Test
    @DisplayName("'doParallel' when exception should throw execution exception")
    void doparallel_when_exception_should_throw_execution_exception() {
        var exception = new RuntimeException("bad");
        var secondResult = new AtomicReference<>();
        var subject = new ParallelFnRunner<String, String>(
                List.of(
                        () -> {
                            throw exception;
                        },
                        () -> {
                            secondResult.set("value2");
                            return "value2";
                        }
                )
        );

        assertThatThrownBy(() -> subject.doParallel(Function.identity()))
                .isInstanceOf(ExecutionException.class)
                .hasCause(exception);
        assertThat(secondResult.get()).isEqualTo("value2");
    }
}
