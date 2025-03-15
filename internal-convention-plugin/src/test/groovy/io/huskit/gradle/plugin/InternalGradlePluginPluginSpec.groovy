package io.huskit.gradle.plugin
//package io.huskit.gradle.plugin
//
//import io.huskit.gradle.plugin.InternalEnvironment
//import io.huskit.gradle.plugin.InternalProperties
//import org.gradle.api.Project
//import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
//import org.gradle.api.tasks.testing.Test
//import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
//import org.gradle.testfixtures.ProjectBuilder
//import org.junit.jupiter.api.Tag
//import spock.lang.Specification
//
//@Tag("integration-test")
//class InternalGradlePluginPluginSpec extends Specification {
//
//    def "plugin should be applied"() {
//        given:
//        Project project = setupProject(isCi)
//
//        when:
//        project.plugins.apply(InternalGradlePluginPlugin)
//
//        then:
//        project.plugins.hasPlugin(InternalGradlePluginPlugin)
//
//        where:
//        isCi << isCi()
//    }
//
//    def "maven local repository should be added on local env"() {
//        given:
//        Project project = setupProject(false)
//
//        when:
//        project.plugins.apply(InternalGradlePluginPlugin)
//
//        then:
//        def repositories = project.repositories.toList()
//        repositories.size() == 2
//        repositories.find({ it.name == 'MavenLocal' }) != null
//        repositories.find({ it.name == 'MavenRepo' }) != null
//    }
//
//    def "maven local repository should not be added on ci env"() {
//        given:
//        Project project = setupProject(true)
//
//        when:
//        project.plugins.apply(InternalGradlePluginPlugin)
//
//        then:
//        def repositories = project.repositories.toList()
//        repositories.size() == 1
//        repositories.find({ it.name == 'MavenRepo' }) != null
//    }
//
//    def "lombok dependencies should be added"() {
//        given:
//        Project project = setupProject(isCi)
//
//        when:
//        project.plugins.apply(InternalGradlePluginPlugin)
//
//        then:
//        def compileDependencies = project.configurations.getByName("compileClasspath").allDependencies
//        compileDependencies.find({ it.name == 'lombok' }) != null
//
//        def annotationProcessorDependencies = project.configurations.getByName("annotationProcessor").allDependencies
//        annotationProcessorDependencies.find({ it.name == 'lombok' }) != null
//
//        where:
//        isCi << isCi()
//    }
//
//    def "spock dependencies should be added"() {
//        given:
//        Project project = setupProject(isCi)
//
//        when:
//        project.plugins.apply(InternalGradlePluginPlugin)
//
//        then:
//        def testImplementationDependencies = project.configurations.getByName("testImplementation").allDependencies
//        testImplementationDependencies.find({ it.name == 'spock-core' }) != null
//        testImplementationDependencies.find({ it.name == 'groovy-all' }) != null
//
//        where:
//        isCi << isCi()
//    }
//
//    def "unit tests should be configured"() {
//        given:
//        Project project = setupProject(isCi)
//
//        when:
//        project.plugins.apply(InternalGradlePluginPlugin)
//
//        then:
//        def integrationTestTask = project.tasks.getByName("unitTest") as Test
//        def framework = integrationTestTask.testFramework as JUnitPlatformTestFramework
//        def options = framework.options as JUnitPlatformOptions
//        options.getIncludeTags().contains("unit-test") == true
//
//        where:
//        isCi << isCi()
//    }
//
//    def "integration tests should be configured"() {
//        given:
//        Project project = setupProject(isCi)
//
//        when:
//        project.plugins.apply(InternalGradlePluginPlugin)
//
//        then:
//        def integrationTestTask = project.tasks.getByName("integrationTest") as Test
//        def framework = integrationTestTask.testFramework as JUnitPlatformTestFramework
//        def options = framework.options as JUnitPlatformOptions
//        options.getIncludeTags().contains("integration-test") == true
//
//        where:
//        isCi << isCi()
//    }
//
//    private List<Boolean> isCi() {
//        return [true, false]
//    }
//
//    private Project setupProject(boolean isCi) {
//        Project project = ProjectBuilder.builder().build()
//        project.extensions.create(InternalEnvironment.EXTENSION_NAME, InternalEnvironment, isCi, true)
//        def ext = project.extensions.getExtraProperties()
//        def internalProperties = project.extensions.create(InternalProperties.EXTENSION_NAME, InternalProperties, project.providers, ext)
//        def lombokVersion = "1.18.20"
//        internalProperties.put("lombokVersion", lombokVersion)
//        def spockVersion = "2.0-groovy-3.0"
//        def groovyVersion = "3.0.9"
//        internalProperties.put("spockVersion", spockVersion)
//        internalProperties.put("groovyVersion", groovyVersion)
//
//
//        return project
//    }
//}
