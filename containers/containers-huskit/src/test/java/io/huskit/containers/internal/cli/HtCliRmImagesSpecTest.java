package io.huskit.containers.internal.cli;

import io.huskit.gradle.commontest.UnitTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtCliRmImagesSpecTest implements UnitTest {

    @Test
    void toCommand__no_args__should_throw_exception() {
        assertThatThrownBy(HtCliRmImagesSpec::new)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image IDs must not be empty");
    }

    @Test
    void toCommand__empty_ids_list__should_throw_exception() {
        assertThatThrownBy(() -> new HtCliRmImagesSpec(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image IDs must not be empty");
    }

    @Test
    void toCommand__with_ids_list__should_return_spec_with_ids() {
        var subject = new HtCliRmImagesSpec(List.of("id1", "id2"));

        var actual = subject.toCommand();

        assertThat(actual).containsExactly("docker", "rmi", "id1", "id2");
    }

    @Test
    void toCommand__with_one_vararg_id__should_return_spec_with_one_id() {
        var subject = new HtCliRmImagesSpec("id1");

        var actual = subject.toCommand();

        assertThat(actual).containsExactly("docker", "rmi", "id1");
    }

    @Test
    void toCommand__with_force_true__should_return_spec_with_force() {
        var subject = new HtCliRmImagesSpec("someid").withForce();

        var actual = subject.toCommand();

        assertThat(actual).containsExactly("docker", "rmi", "--force", "someid");
    }

    @Test
    void toCommand__with_no_prune_true__should_return_spec_with_no_prune() {
        var subject = new HtCliRmImagesSpec("someid").withNoPrune();

        var actual = subject.toCommand();

        assertThat(actual).containsExactly("docker", "rmi", "--no-prune", "someid");
    }

    @Test
    void toCommand__with_force_true_and_no_prune_true__should_return_spec_with_force_and_no_prune() {
        var subject = new HtCliRmImagesSpec("someid").withForce().withNoPrune();

        var actual = subject.toCommand();

        assertThat(actual).containsExactly("docker", "rmi", "--force", "--no-prune", "someid");
    }

    @Test
    void toCommand__with_no_prune_true_and_force_true__should_return_spec_with_force_and_no_prune() {
        var subject = new HtCliRmImagesSpec("someid").withNoPrune().withForce();

        var actual = subject.toCommand();

        assertThat(actual).containsExactly("docker", "rmi", "--force", "--no-prune", "someid");
    }
}