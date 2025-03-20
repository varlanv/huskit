package io.huskit.common.reactive;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OneTest implements UnitTest {

    @Test
    @DisplayName("`from item -> map -> map -> toFuture -> join` should produce correct value")
    void from_item_map_map_tofuture_join_should_produce_correct_value() {
        var actual = One.from()
            .item("string")
            .map(String::toUpperCase)
            .map(string -> string.substring(0, 1))
            .toFuture()
            .join();

        assertThat(actual).isEqualTo("S");
    }

    @Test
    @DisplayName("`from item -> flatmap from item -> map -> flatmap from item -> toFuture -> join` should produce correct value")
    void from_item_flatmap_from_item_map_flatmap_from_item_tofuture_join_should_produce_correct_value() {
        var actual = One.from()
            .item("string")
            .flatMap(string -> One.from().item(string.toUpperCase()))
            .map(string -> string.substring(0, 1))
            .flatMap(string -> One.from().item(string.repeat(2)))
            .toFuture()
            .join();

        assertThat(actual).isEqualTo("SS");
    }

    @Test
    @DisplayName("`from item -> flatmap from item -> map -> flatmap from item -> subscribe` should produce correct value synchronously")
    void from_item_flatmap_from_item_map_flatmap_from_item_subscribe_should_produce_correct_value_synchronously() {
        var actual = new CompletableFuture<String>();
        One.from()
            .item("string")
            .flatMap(string -> One.from().item(string.toUpperCase()))
            .map(string -> string.substring(0, 1))
            .flatMap(string -> One.from().item(string.repeat(2)))
            .subscribe(actual::complete);

        assertThat(actual).isCompletedWithValue("SS");
    }

    @Test
    @DisplayName("`from item -> map -> map -> mapToNothing -> subscribe` should run whole chain")
    void from_item_map_map_maptonothing_subscribe_should_run_whole_chain() {
        var actual = new CompletableFuture<String>();
        One.from()
            .item("string")
            .map(String::toUpperCase)
            .map(string -> {
                var substring = string.substring(0, 1);
                actual.complete(substring);
                return substring;
            })
            .mapToNothing()
            .subscribe(never -> {
            });

        assertThat(actual).isCompletedWithValue("S");
    }

    @Test
    @DisplayName("`from item -> map -> map -> mapToNothing -> map -> subscribe` should run whole chain")
    void from_item_map_map_maptonothing_map_subscribe_should_run_whole_chain() {
        var actual = new CompletableFuture<String>();
        One.from()
            .item("string")
            .map(String::toUpperCase)
            .map(string -> string.substring(0, 1))
            .mapToNothing()
            .map(never -> "newString")
            .subscribe(actual::complete);

        assertThat(actual).isCompletedWithValue("newString");
    }

    @Test
    @DisplayName("`from item -> map -> runOnComplete -> map -> subscribe` should run whole chain")
    void from_item_map_runoncomplete_map_subscribe_should_run_whole_chain() {
        var actual = new CompletableFuture<String>();
        var actualOnComplete = new CompletableFuture<String>();
        One.from()
            .item("string")
            .map(String::toUpperCase)
            .runOnComplete(() -> actualOnComplete.complete("newString"))
            .map(string -> string.substring(0, 1))
            .subscribe(actual::complete);

        assertThat(actual).isCompletedWithValue("S");
        assertThat(actualOnComplete).isCompletedWithValue("newString");
    }

    @Test
    @DisplayName("`from item -> runOnComplete -> flatMap from error -> map -> subscribe` should run on complete callback")
    void from_item_runoncomplete_flatmap_from_error_map_subscribe_should_run_on_complete_callback() {
        var actual = new CompletableFuture<String>();
        var actualOnComplete = new CompletableFuture<String>();
        assertThatThrownBy(() ->
            One.from()
                .item("string")
                .runOnComplete(() -> actualOnComplete.complete("newString"))
                .<String>flatMap(string -> One.from().error(new RuntimeException("fail")))
                .map(string -> string.substring(0, 1))
                .subscribe(actual::complete)
        ).hasMessage("fail");

        assertThat(actual).isNotCompleted();
        assertThat(actualOnComplete).isCompletedWithValue("newString");
    }

    @Test
    @DisplayName("`from item -> flatMap from error -> runOnComplete -> map -> subscribe` should not run on complete callback")
    void from_item_flatmap_from_error_runoncomplete_map_subscribe_should_not_run_on_complete_callback() {
        var actual = new CompletableFuture<String>();
        var actualOnComplete = new CompletableFuture<String>();
        assertThatThrownBy(() ->
            One.from()
                .item("string")
                .<String>flatMap(string -> One.from().error(new RuntimeException("fail")))
                .runOnComplete(() -> actualOnComplete.complete("newString"))
                .map(string -> string.substring(0, 1))
                .subscribe(actual::complete)
        ).hasMessage("fail");

        assertThat(actual).isNotCompleted();
        assertThat(actualOnComplete).isNotCompleted();
    }

    @Test
    @DisplayName("`from completed future -> map -> subscribe` should produce correct value synchronously")
    void from_completed_future_map_subscribe_should_produce_correct_value_synchronously() {
        var actual = new CompletableFuture<String>();
        One.from()
            .completion(CompletableFuture.completedFuture("string"))
            .map(string -> string.substring(0, 1))
            .subscribe(actual::complete);

        assertThat(actual).isCompletedWithValue("s");
    }

    @Test
    @DisplayName("`from not completed future -> map -> subscribe` does not produce value")
    void from_not_completed_future_map_subscribe_does_not_produce_value() {
        var actual = new CompletableFuture<String>();
        var completableFuture = new CompletableFuture<String>();
        One.from()
            .completion(completableFuture)
            .map(string -> string.substring(0, 1))
            .subscribe(actual::complete);

        assertThat(actual).isNotCompleted();
    }

    @Test
    @DisplayName("`from not completed future -> map -> subscribe`  produces value after future completes")
    void from_not_completed_future_map_subscribe_produces_value_after_future_completes() {
        var actual = new CompletableFuture<String>();
        var completableFuture = new CompletableFuture<String>();
        One.from()
            .completion(completableFuture)
            .map(string -> string.substring(0, 1))
            .subscribe(actual::complete);

        completableFuture.complete("string");
        assertThat(actual).isCompletedWithValue("s");
    }

    @Test
    @DisplayName("`from emitter -> map -> subscribe` produces value when emitter emits success")
    void from_emitter_map_subscribe_produces_value_when_emitter_emits_success() {
        var actual = new CompletableFuture<String>();
        One.from().<String>emitter(emitter -> emitter.complete("string"))
            .map(string -> string.substring(0, 1))
            .subscribe(actual::complete);
        assertThat(actual).isCompletedWithValue("s");
    }


    @Test
    @DisplayName("`from failing emitter -> map -> subscribe` should throw error")
    void from_failing_emitter_map_subscribe_should_throw_error() {
        var actual = new CompletableFuture<String>();
        var error = new RuntimeException("fail");

        assertThatThrownBy(() ->
            One.from().<String>emitter(emitter -> emitter.fail(error))
                .map(string -> string.substring(0, 1))
                .subscribe(actual::complete)
        ).isSameAs(error);
    }

    @Test
    @DisplayName("`from error -> map -> subscribe` should throw error")
    void from_error_map_subscribe_should_throw_error() {
        var actual = new CompletableFuture<String>();
        var actualOnComplete = new CompletableFuture<String>();
        assertThatThrownBy(() ->
            One.from()
                .item("string")
                .<String>flatMap(string -> One.from().error(new RuntimeException("fail")))
                .runOnComplete(() -> actualOnComplete.complete("newString"))
                .map(string -> string.substring(0, 1))
                .subscribe(actual::complete)
        ).hasMessage("fail");

        assertThat(actual).isNotCompleted();
        assertThat(actualOnComplete).isNotCompleted();
    }

    @Test
    @DisplayName("`from error -> subscribe` should throw error")
    void from_error_subscribe_should_throw_error() {
        var actual = new CompletableFuture<>();
        var error = new RuntimeException("fail");

        assertThatThrownBy(() ->
            One.from().error(error)
                .subscribe(actual::complete)
        ).isSameAs(error);
    }

    @Test
    @DisplayName("`from item -> flatMapMany` should return many")
    void from_item_flatmapmany_should_return_many() {
        var actual = One.from()
            .item("string")
            .flatMapMany(string -> Many.from().items(string + 1, string + 2))
            .list();

        assertThat(actual).containsExactly("string1", "string2");
    }
}
