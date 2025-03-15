pluginManagement {
    repositories {
        if (providers.environmentVariable("CI").getOrNull() == null) {
            mavenLocal()
        }
        gradlePluginPortal()
    }
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention").version(providers.gradleProperty("foojayToolchainPluginVersion").get())
        id("com.gradleup.shadow").version(providers.gradleProperty("shadowPluginVersion").get())
    }
    includeBuild("internal-convention-plugin")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

rootProject.name = "huskit"

val isCi = providers.environmentVariable("CI").getOrNull()?.let { it != "false" } ?: false

buildCache {
    local {
        isEnabled = !isCi
        isPush = !isCi
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeBuild("internal-convention-plugin")

include(
    "common",
    "common-test",
    "functional-test",
    "plugins:common-plugin",
    "containers:containers-core",
    "containers:containers-huskit",
    "plugins:containers-plugin:containers-gradle-plugin",
)
