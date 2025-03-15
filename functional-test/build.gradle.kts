plugins {
    `java-gradle-plugin`
    alias(libs.plugins.internalConvention)
}

internalConvention {
    integrationTestName = "functionalTest"
}

dependencies {
    implementation(projects.common)
    implementation(projects.commonTest)
    implementation(projects.containers.containersHuskit)
    implementation(projects.plugins.commonPlugin)
    implementation(projects.plugins.containersPlugin.containersGradlePlugin)
    testImplementation(libs.apache.commons.lang)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(gradleTestKit())
}
