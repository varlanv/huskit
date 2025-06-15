plugins {
    `java-gradle-plugin`
}

val isCiBuild = providers.environmentVariable("CI").orNull != null

if (isCiBuild) {
    java {
        version = JavaVersion.VERSION_17
    }
} else {
    java {
        toolchain {
            vendor.set(JvmVendorSpec.AZUL)
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}

repositories {
    if (!isCiBuild) {
        mavenLocal()
    }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.benmanes.version.plugin)
    if (!isCiBuild) {
        implementation("com.varlanv.test-konvence:com.varlanv.test-konvence.gradle.plugin:0.0.1")
    }
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.lombok)
    implementation(libs.junit.platform.launcher)
    annotationProcessor(libs.lombok)
}

gradlePlugin {
    plugins {
        create("huskitInternalGradleConventionPlugin") {
            id = libs.plugins.internalConvention.get().pluginId
            implementationClass = "io.huskit.gradle.plugin.InternalConventionPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
