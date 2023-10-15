package io.huskit.gradle.common.plugin.model

import io.huskit.gradle.common.plugin.model.props.Props
import io.huskit.gradle.common.plugin.model.props.fake.FakeProps
import io.huskit.gradle.commontest.BaseIntegrationSpec
import io.huskit.log.fake.FakeLog

class NewOrExistingExtensionIntegrationSpec extends BaseIntegrationSpec {

    def "if extension not exists, then should create"() {
        given:
        def project = setupProject()
        def log = new FakeLog()
        def subject = new NewOrExistingExtension(log, project.extensions)

        when:
        def actual = subject.getOrCreate(Props, FakeProps, Props.EXTENSION_NAME)

        then:
        actual != null
        project.extensions.findByName(Props.EXTENSION_NAME) === actual

        and:
        def loggedMessages = log.loggedMessages()
        loggedMessages.size() == 1
        def loggedMessage = loggedMessages[0]
        loggedMessage.args.size() == 1
        loggedMessage.args[0] == Props.EXTENSION_NAME
        loggedMessage.message.contains("not found, creating new instance")

    }

    def "if extension exists, then should return existing"() {
        given:
        def project = setupProject()
        def log = new FakeLog()
        def subject = new NewOrExistingExtension(log, project.extensions)
        def expected = new FakeProps()
        project.extensions.add(Props.EXTENSION_NAME, expected)

        when:
        def actual = subject.getOrCreate(Props, FakeProps, Props.EXTENSION_NAME)

        then:
        actual === expected

        and:
        def loggedMessages = log.loggedMessages()
        loggedMessages.size() == 1
        def loggedMessage = loggedMessages[0]
        loggedMessage.args.size() == 1
        loggedMessage.args[0] == Props.EXTENSION_NAME
        loggedMessage.message.contains("found, using existing instance")
    }
}
