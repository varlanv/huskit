plugins {
    `maven-publish`
    `java-library`
    id("io.huskit.gradle.internal-gradle-convention-plugin")
}

dependencies {
    implementation(projects.common)
    implementation(projects.containers.containersModel)
    implementation(projects.logging.loggingApi)
}
