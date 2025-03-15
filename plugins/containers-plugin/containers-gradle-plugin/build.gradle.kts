plugins {
    alias(libs.plugins.internalConvention)
}

gradlePlugin {
    plugins {
        create("huskitContainersGradlePlugin") {
            id = "io.huskit.gradle.containers-plugin"
            implementationClass = "io.huskit.gradle.containers.plugin.HuskitContainersPlugin"
        }
    }
}

dependencies {
    implementation(projects.common)
    implementation(projects.containers.containersCore)
    implementation(projects.containers.containersHuskit)
    implementation(projects.plugins.commonPlugin)
    testImplementation(libs.mockito.core)
    testImplementation(libs.bundles.testcontainers)
}
