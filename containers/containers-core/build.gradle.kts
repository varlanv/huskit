plugins {
    `maven-publish`
    `java-library`
    alias(libs.plugins.internalConvention)
}

dependencies {
    implementation(libs.mutiny)
    implementation(projects.common)
    implementation(projects.containers.containersHuskit)
    testImplementation(libs.bundles.testcontainers)
}
