package io.huskit.gradle.common.plugin.model.props;

import io.huskit.common.Log;
import io.huskit.gradle.common.plugin.model.NewOrExistingExtension;
import io.huskit.gradle.common.plugin.model.props.fake.FakeProps;
import io.huskit.gradle.common.plugin.model.props.fake.RandomizeIfEmptyProps;
import io.huskit.gradle.commontest.GradleIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReplaceablePropsIntegrationTest implements GradleIntegrationTest {

    @Test
    @DisplayName("'hasProp' should return true if existing extension has property")
    void hasprop_should_return_true_if_existing_extension_has_property() {
        runProjectFixture(
            fixture -> {
                var log = Log.fake();
                var subject = prepareSubjectAndExtension(fixture, log);

                assertThat(subject.hasProp("any")).isTrue();

                var logMessages = log.infoMessages();
                assertThat(logMessages).hasSize(1);
                var logMessage = logMessages.get(0);
                assertThat(logMessage).contains("found, using existing instance");
            }
        );
    }

    @Test
    @DisplayName("'hasProp' should return false if existing extension has no property")
    void hasprop_should_return_false_if_existing_extension_has_no_property() {
        runProjectFixture(
            fixture -> {
                var log = Log.fake();
                var subject = prepareSubject(fixture, log);

                assertThat(subject.hasProp("any")).isFalse();

                var logMessages = log.infoMessages();
                assertThat(logMessages).hasSize(1);
                var logMessage = logMessages.get(0);
                assertThat(logMessage).contains("not found, creating new instance");
            }
        );
    }

    private ReplaceableProps prepareSubjectAndExtension(SingleProjectFixture fixture, Log log) {
        fixture.project().getExtensions().add(Props.class, Props.name(), new RandomizeIfEmptyProps(new FakeProps()));
        return prepareSubject(fixture, log);
    }

    private ReplaceableProps prepareSubject(SingleProjectFixture fixture, Log log) {
        return new ReplaceableProps(
            fixture.project().getProviders(),
            fixture.project().getExtensions().getExtraProperties(),
            new NewOrExistingExtension(
                log,
                fixture.project().getExtensions()
            )
        );
    }
}
