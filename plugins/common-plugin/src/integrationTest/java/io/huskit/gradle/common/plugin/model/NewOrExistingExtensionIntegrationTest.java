package io.huskit.gradle.common.plugin.model;

import io.huskit.gradle.common.plugin.model.props.Props;
import io.huskit.gradle.common.plugin.model.props.fake.FakeProps;
import io.huskit.gradle.commontest.GradleIntegrationTest;
import io.huskit.log.fake.FakeLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NewOrExistingExtensionIntegrationTest implements GradleIntegrationTest {

    @Test
    @DisplayName("if extension not exists, then should create")
    void if_extension_not_exists_then_should_create() {
        runProjectFixture(fixture -> {
            var project = fixture.project();
            var log = new FakeLog();
            var subject = new NewOrExistingExtension(log, project.getExtensions());

            var actual = subject.getOrCreate(Props.class, FakeProps.class, Props.name());

            assertThat(actual).isNotNull();
            assertThat(project.getExtensions().findByName(Props.name())).isSameAs(actual);
            var loggedMessages = log.loggedMessages();
            assertThat(loggedMessages).hasSize(1);
            var loggedMessage = loggedMessages.get(0);
            assertThat(loggedMessage.args()).hasSize(1);
            assertThat(loggedMessage.args().get(0)).isEqualTo(Props.name());
            assertThat(loggedMessage.message()).contains("not found, creating new instance");
        });
    }

    @Test
    @DisplayName("if extension exists, then should return existing")
    void if_extension_exists_then_should_return_existing() {
        runProjectFixture(fixture -> {
            var project = fixture.project();
            var log = new FakeLog();
            var subject = new NewOrExistingExtension(log, project.getExtensions());
            var expected = new FakeProps();
            project.getExtensions().add(Props.name(), expected);

            var actual = subject.getOrCreate(Props.class, FakeProps.class, Props.name());

            assertThat(actual).isSameAs(expected);
            var loggedMessages = log.loggedMessages();
            assertThat(loggedMessages).hasSize(1);
            var loggedMessage = loggedMessages.get(0);
            assertThat(loggedMessage.args()).hasSize(1);
            assertThat(loggedMessage.args().get(0)).isEqualTo(Props.name());
            assertThat(loggedMessage.message()).contains("found, using existing instance");
        });
    }
}
