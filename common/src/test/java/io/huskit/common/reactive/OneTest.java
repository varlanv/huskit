package io.huskit.common.reactive;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

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
}
