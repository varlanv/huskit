package io.huskit.containers.internal;

import io.huskit.containers.api.container.HtJsonContainer;
import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtJsonContainerTest implements UnitTest {

    @Test
    @DisplayName("id should get id from map")
    void id_should_get_id_from_map() {
        // given
        var container = new HtJsonContainer(Map.of("Id", "someId"));

        // then
        assertThat(container.id()).isEqualTo("someId");
    }

    @Test
    @DisplayName("id if id is null should throw exception")
    void id_if_id_is_null_should_throw_exception() {
        // given
        var container = new HtJsonContainer(Map.of());

        // then
        assertThatThrownBy(container::id)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContainingAll("Id");
    }

    @Test
    @DisplayName("name should get name from map")
    void name_should_get_name_from_map() {
        // given
        var container = new HtJsonContainer(Map.of("Name", "someName"));

        // then
        assertThat(container.name()).isEqualTo("someName");
    }

    @Test
    @DisplayName("name if name is null should throw exception")
    void name_if_name_is_null_should_throw_exception() {
        // given
        var container = new HtJsonContainer(Map.of());

        // then
        assertThatThrownBy(container::name)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContainingAll("Name");
    }

    @Test
    @DisplayName("labels should get labels from map")
    void labels_should_get_labels_from_map() {
        // given
        var container = new HtJsonContainer(Map.of(
            "Config", Map.of("Labels", Map.of("key", "value"))
        ));

        // then
        assertThat(container.config().labels()).containsOnly(Map.entry("key", "value"));
    }

    @Test
    @DisplayName("labels if labels is null should throw exception")
    void labels_if_labels_is_null_should_throw_exception() {
        // given
        var container = new HtJsonContainer(Map.of(
            "Config", Map.of()
        ));

        // then
        assertThatThrownBy(container.config()::labels)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContainingAll("Labels");
    }

    @Test
    @DisplayName("labels if config is null should throw exception")
    void labels_if_config_is_null_should_throw_exception() {
        // given
        var container = new HtJsonContainer(Map.of());

        // then
        assertThatThrownBy(() -> container.config().labels())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContainingAll("Config");
    }
}
