package io.huskit.common;

import io.huskit.common.function.ThrowingSupplier;
import io.huskit.gradle.commontest.UnitTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DfVolatileTest implements UnitTest {

    String subjectValue = "value";

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("set when value was null sets value")
    void set_when_value_was_null_sets_value(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        subject.set(subjectValue);

        assertThat(subject.require()).isEqualTo(subjectValue);
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("set when switching value sets new value")
    void set_when_switching_value_sets_new_value(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        subject.set(subjectValue);
        subject.set("new value");

        assertThat(subject.require()).isEqualTo("new value");
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("set when pass null throws exception")
    void set_when_pass_null_throws_exception(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThatThrownBy(() -> subject.set(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(subjectValue);
    }

    @Test
    @DisplayName("reset when value was set resets value")
    void reset_when_value_was_set_resets_value() {
        var subject = new DfVolatile<String>();
        subject.set(subjectValue);

        subject.reset();

        assertThat(subject.isPresent()).isFalse();
    }

    @Test
    @DisplayName("reset when value was not set does nothing")
    void reset_when_value_was_not_set_does_nothing() {
        var subject = new DfVolatile<String>();

        subject.reset();

        assertThat(subject.get()).isNull();
    }

    @Test
    @DisplayName("get when value was set returns value")
    void get_when_value_was_set_returns_value() {
        var subject = new DfVolatile<String>();
        subject.set(subjectValue);

        assertThat(subject.get()).isEqualTo(subjectValue);
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("require when value was set returns value")
    void require_when_value_was_set_returns_value(Supplier<Mutable<String>> factory) {
        var subject = factory.get();
        subject.set(subjectValue);

        assertThat(subject.require()).isEqualTo(subjectValue);
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("require when value was not set throws exception")
    void require_when_value_was_not_set_throws_exception(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThatThrownBy(subject::require)
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("No value present");
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("'ifPresent' when value was set executes consumer")
    void ifpresent_when_value_was_set_executes_consumer(Supplier<Mutable<String>> factory) {
        var subject = factory.get();
        subject.set(subjectValue);
        var wasCalled = new AtomicBoolean();

        subject.ifPresent(val -> {
            assertThat(val).isEqualTo(subjectValue);
            wasCalled.set(true);
        });
        assertThat(wasCalled).isTrue();
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("maybe when value was set returns optional with value")
    void maybe_when_value_was_set_returns_optional_with_value(Supplier<Mutable<String>> factory) {
        var subject = factory.get();
        subject.set(subjectValue);

        assertThat(subject.maybe()).contains(subjectValue);
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("maybe when value was not set returns empty optional")
    void maybe_when_value_was_not_set_returns_empty_optional(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThat(subject.maybe()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("'isEmpty' when value was set returns false")
    void isempty_when_value_was_set_returns_false(Supplier<Mutable<String>> factory) {
        var subject = factory.get();
        subject.set(subjectValue);

        assertThat(subject.isEmpty()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("'isEmpty' when value was not set returns true")
    void isempty_when_value_was_not_set_returns_true(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThat(subject.isEmpty()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("'isPresent' when value was set returns true")
    void ispresent_when_value_was_set_returns_true(Supplier<Mutable<String>> factory) {
        var subject = factory.get();
        subject.set(subjectValue);

        assertThat(subject.isPresent()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("'isPresent' when value was not set returns false")
    void ispresent_when_value_was_not_set_returns_false(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThat(subject.isPresent()).isFalse();
    }

    @Test
    @DisplayName("if two threads call 'syncSetOrGet' at the same time computation should be performed only once")
    void if_two_threads_call_syncsetorget_at_the_same_time_computation_should_be_performed_only_once() throws Exception {
        var subject = new DfVolatile<String>();
        var counter = new AtomicInteger();
        var threadsReadyLatch = new CountDownLatch(2);
        var caseReadyLatch = new CountDownLatch(1);
        var threadsFinishedLatch = new CountDownLatch(2);
        var valueSupplier = new ThrowingSupplier<String>() {
            @Override
            @SneakyThrows
            public String get() {
                counter.incrementAndGet();
                return subjectValue;
            }
        };
        Runnable runnable = () -> {
            threadsReadyLatch.countDown();
            try {
                caseReadyLatch.await();
            } catch (InterruptedException e) {
                throw hide(e);
            }
            subject.syncSetOrGet(valueSupplier);
            threadsFinishedLatch.countDown();
        };
        CompletableFuture.runAsync(runnable);
        CompletableFuture.runAsync(runnable);
        threadsReadyLatch.await();
        caseReadyLatch.countDown();
        threadsFinishedLatch.await();

        assertThat(counter.get()).isEqualTo(1);
        assertThat(subject.get()).isEqualTo(subjectValue);
    }

    @Test
    @DisplayName("'syncSetOrGet' if value is already set returns value")
    void syncsetorget_if_value_is_already_set_returns_value() throws Exception {
        var subject = new DfVolatile<String>();
        subject.set(subjectValue);

        var result = subject.syncSetOrGet(() -> {
            throw new IllegalStateException("Should not be called");
        });

        assertThat(result).isEqualTo(subjectValue);
    }

    @Test
    @DisplayName("'syncSetOrGet' if supplier throws exception should not be ignored")
    void syncsetorget_if_supplier_throws_exception_should_not_be_ignored() {
        var subject = new DfVolatile<String>();
        var exception = new IllegalStateException("msg");

        assertThatThrownBy(() ->
                subject.syncSetOrGet(() -> {
                    throw exception;
                })
        ).isSameAs(exception);
    }

    @Test
    @DisplayName("when constructing from another volatile copies value")
    void when_constructing_from_another_volatile_copies_value() {
        var another = new DfVolatile<String>();
        another.set(subjectValue);

        var subject = new DfVolatile<>(another);

        assertThat(subject.get()).isEqualTo(subjectValue);
    }

    @Test
    @DisplayName("when constructing from empty volatile copies nothing")
    void when_constructing_from_empty_volatile_copies_nothing() {
        var another = new DfVolatile<String>();

        var subject = new DfVolatile<>(another);

        assertThat(subject.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("when constructing from value use value")
    void when_constructing_from_value_use_value() {
        var subject = new DfVolatile<>(subjectValue);

        assertThat(subject.get()).isEqualTo(subjectValue);
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("or when value is present returns value")
    void or_when_value_is_present_returns_value(Supplier<Mutable<String>> factory) {
        var subject = factory.get();
        subject.set(subjectValue);

        assertThat(subject.or("other")).isEqualTo(subjectValue);
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("or when value is not present returns other")
    void or_when_value_is_not_present_returns_other(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThat(subject.or("other")).isEqualTo("other");
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("or when other is null and value is present returns value")
    void or_when_other_is_null_and_value_is_present_returns_value(Supplier<Mutable<String>> factory) {
        var subject = factory.get();
        subject.set(subjectValue);

        assertThat(subject.or((String) null)).isEqualTo(subjectValue);
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("or when other is null and value is not present throws exception")
    void or_when_other_is_null_and_value_is_not_present_throws_exception(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThatThrownBy(() -> subject.or((String) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("other");
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("or when supplier is present returns value")
    void or_when_supplier_is_present_returns_value(Supplier<Mutable<String>> factory) {
        var subject = factory.get();
        subject.set(subjectValue);

        assertThat(subject.or(() -> "other")).isEqualTo(subjectValue);
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("or when supplier is not present returns other")
    void or_when_supplier_is_not_present_returns_other(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThat(subject.or(() -> "other")).isEqualTo("other");
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("or when supplier is null and value is present returns value")
    void or_when_supplier_is_null_and_value_is_present_returns_value(Supplier<Mutable<String>> factory) {
        var subject = factory.get();
        subject.set(subjectValue);

        assertThat(subject.or((ThrowingSupplier<String>) null)).isEqualTo(subjectValue);
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("or when supplier is null and value is not present throws exception")
    void or_when_supplier_is_null_and_value_is_not_present_throws_exception(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThatThrownBy(() -> subject.or((ThrowingSupplier<String>) null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("or when supplier throws exception should not be ignored")
    void or_when_supplier_throws_exception_should_not_be_ignored(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThatThrownBy(() -> subject.or(() -> {
            throw new IOException("msg");
        })).isInstanceOf(IOException.class).hasMessage("msg");
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("or when supplier returns null throws exception")
    void or_when_supplier_returns_null_throws_exception(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThatThrownBy(() -> subject.or(() -> null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("supplier");
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("check when value is present and predicate returns true returns true")
    void check_when_value_is_present_and_predicate_returns_true_returns_true(Supplier<Mutable<String>> factory) {
        var subject = factory.get();
        subject.set(subjectValue);

        assertThat(subject.check(val -> val.equals(subjectValue))).isTrue();
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("check when value is present and predicate returns false returns false")
    void check_when_value_is_present_and_predicate_returns_false_returns_false(Supplier<Mutable<String>> factory) {
        var subject = factory.get();
        subject.set(subjectValue);

        assertThat(subject.check("other"::equals)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("emptyMutableFactories")
    @DisplayName("check when value is not present returns false and does not call predicate")
    void check_when_value_is_not_present_returns_false_and_does_not_call_predicate(Supplier<Mutable<String>> factory) {
        var subject = factory.get();

        assertThat(subject.check(val -> {
            throw new IllegalStateException("Should not be called");
        })).isFalse();
    }

    @ParameterizedTest
    @MethodSource("mutableFactories")
    @DisplayName("check when predicate throws exception should not be ignored")
    void check_when_predicate_throws_exception_should_not_be_ignored(Function<String, Mutable<String>> factory) {
        var subject = factory.apply("value");

        assertThatThrownBy(() ->
                subject.check(val -> {
                    throw new IOException("msg");
                })
        ).isInstanceOf(IOException.class).hasMessage("msg");
    }

    Stream<Arguments> mutableFactories() {
        return Stream.of(
                Arguments.of((Function<String, Mutable<String>>) DfVolatile::new),
                Arguments.of((Function<String, Mutable<String>>) DfMutable::new)
        );
    }

    Stream<Arguments> emptyMutableFactories() {
        return Stream.of(
                Arguments.of((Supplier<Mutable<String>>) DfVolatile::new),
                Arguments.of((Supplier<Mutable<String>>) DfMutable::new)
        );
    }
}
