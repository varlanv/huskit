package io.huskit.gradle.common.plugin.model.props

import io.huskit.gradle.common.plugin.model.NewOrExistingExtension
import io.huskit.gradle.common.plugin.model.props.fake.FakeProps
import io.huskit.gradle.common.plugin.model.props.fake.RandomizeIfEmptyProps
import io.huskit.gradle.commontest.BaseIntegrationSpec
import io.huskit.log.Log
import io.huskit.log.fake.FakeLog
import org.gradle.api.Project

class ReplaceablePropsIntegrationSpec extends BaseIntegrationSpec {

    def "'hasProp' should return true if existing extension has property"() {
        given:
        def project = setupProject()
        def log = new FakeLog()
        def subject = prepareSubjectAndExtension(project, log)

        expect:
        subject.hasProp("any") == true

        and:
        log.loggedMessages().size() == 1
        def loggedMessage = log.loggedMessages()[0]
        loggedMessage.args.size() == 1
        loggedMessage.args[0] == Props.EXTENSION_NAME
        loggedMessage.message.contains("found, using existing instance")
    }

    def "'hasProp' should return false if existing extension has no property"() {
        given:
        def project = setupProject()
        def log = new FakeLog()
        def subject = prepareSubject(log)

        expect:
        subject.hasProp("any") == false

        and:
        log.loggedMessages().size() == 1
        def loggedMessage = log.loggedMessages()[0]
        loggedMessage.args.size() == 1
        loggedMessage.args[0] == Props.EXTENSION_NAME
        loggedMessage.message.contains("not found, creating new instance")
    }

    private ReplaceableProps prepareSubjectAndExtension(Project project, Log log) {
        project.extensions.add(Props, Props.EXTENSION_NAME, new RandomizeIfEmptyProps(new FakeProps()))
        return prepareSubject(log)
    }

    private ReplaceableProps prepareSubject(Log log) {
        def subject = new ReplaceableProps(
                project.providers,
                project.extensions.extraProperties,
                new NewOrExistingExtension(
                        log,
                        project.extensions
                )
        )
        return subject
    }
}
