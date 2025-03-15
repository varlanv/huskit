plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.internalConvention)
}

dependencies {
    implementation(libs.mutiny)
}
