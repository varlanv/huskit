plugins {
    alias(libs.plugins.internalConvention)
}

gradlePlugin {
    plugins {
        create("huskitCommonGradlePlugin") {
            id = "io.huskit.gradle.common-plugin"
            implementationClass = "io.huskit.gradle.common.plugin.HuskitCommonPlugin"
        }
    }
}

dependencies {
    implementation(projects.common)
}
