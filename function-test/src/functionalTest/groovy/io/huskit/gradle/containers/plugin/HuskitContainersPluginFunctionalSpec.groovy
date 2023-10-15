package io.huskit.gradle.containers.plugin

import io.huskit.gradle.commontest.BaseFunctionalSpec
import io.huskit.gradle.commontest.DataTable
import io.huskit.gradle.commontest.DataTables
import io.huskit.gradle.containers.plugin.api.ContainersExtension
import spock.lang.Subject

@Subject(HuskitContainersPlugin)
class HuskitContainersPluginFunctionalSpec extends BaseFunctionalSpec {

    def "should add 'serviceContainers' extension"() {
        given:
        setupFixture()
        def runner = prepareGradleRunner(dataTable, "help", "--info")
        rootBuildFile.text = """
        plugins {
            id "io.huskit.gradle.containers-plugin"
        }
"""

        when:
        def buildResult = build(runner)

        then:
        buildResult.output.contains(ContainersExtension.name())

        where:
        dataTable << DataTables.default.get()
    }

    def "apply-plugin-to-multiple-java-projects-all-reusable should work correctly"() {
        expect:
        runUseCase("apply-plugin-to-multiple-java-projects-all-reusable", dataTable)

        where:
        dataTable << DataTables.default.get()
    }

    def "apply-plugin-to-multiple-java-projects-all-not-reusable should work correctly"() {
        expect:
        runUseCase("apply-plugin-to-multiple-java-projects-all-not-reusable", dataTable)

        where:
        dataTable << DataTables.default.get()
    }

    def "apply-plugin-to-single-java-project should work correctly"() {
        expect:
        runUseCase("apply-plugin-to-single-java-project", dataTable)

        where:
        dataTable << DataTables.default.get()
    }

    def "apply-plugin-to-single-java-project should work correctly"() {
        expect:
        runUseCase("apply-plugin-to-multiple-java-projects-all-not-reusable", dataTable)

        where:
        dataTable << DataTables.default.get()
    }

    void runUseCase(String useCaseName, DataTable dataTable) {
        def testCaseDir = useCaseDir(useCaseName)
        copyFolderContents(testCaseDir.absolutePath, testProjectDir.absolutePath)
        def runner = prepareGradleRunner(dataTable, "clean", "check")
                .withEnvironment(["FUNCTIONAL_SPEC_RUN": 'true'])

        build(runner)
        build(runner)
    }

    private File useCaseDir(String useCaseDirName) {
        return new File(
                new File(
                        new File(
                                useCasesDir(),
                                "plugins"
                        ),
                        "containers-plugin"
                ),
                useCaseDirName
        )
    }
}
