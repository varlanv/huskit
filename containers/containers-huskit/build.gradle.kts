import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `maven-publish`
    `java-library`
    alias(libs.plugins.internalConvention)
    alias(libs.plugins.shadow)
}

tasks.named<ShadowJar>("shadowJar", {
    dependencies {
        exclude(dependency("org.slf4j:slf4j-api"))
    }
    relocate("io.huskit.common", "io.huskit.shadow.common")
    relocate("io.huskit.log", "io.huskit.shadow.log")
    relocate("org.json", "io.huskit.shadow.json")
    minimize()
})

dependencies {
    implementation(projects.common)
    implementation(libs.mutiny)
    implementation(libs.json)
    testImplementation(libs.github.docker.java)
    testImplementation(libs.github.docker.transport)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.mongoDriver)
}
