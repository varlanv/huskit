plugins {
    `maven-publish`
    `java-library`
    id("io.huskit.gradle.internal-gradle-convention-plugin")
}

dependencies {
    implementation(projects.logging.loggingApi)
    implementation(projects.common)
    implementation(projects.containers.containersModel)
    implementation(libs.bundles.testcontainers)
}