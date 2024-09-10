package io.huskit.gradle.plugin.internal;

import io.huskit.gradle.plugin.HuskitInternalConventionExtension;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JvmVendorSpec;
import org.gradle.testing.base.TestingExtension;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class ConfigureTests {

    ExtensionContainer extensions;
    HuskitInternalConventionExtension huskitConventionExtension;
    ConfigurationContainer configurations;
    PluginManager pluginManager;
    TaskContainer tasks;
    JavaToolchainService javaToolchainService;
    Provider<TestSynchronizerBuildService> syncBuildService;

    @SuppressWarnings("UnstableApiUsage")
    public void configure() {
        pluginManager.withPlugin("java", plugin -> {
            var testing = (TestingExtension) extensions.getByName("testing");
            var suites = testing.getSuites();
            var integrationTestTaskName = huskitConventionExtension.getIntegrationTestName().get();
            var integrationTestSuite = suites.register(
                    integrationTestTaskName,
                    JvmTestSuite.class,
                    suite ->
                            suite.getTargets().all(target ->
                                    target.getTestTask().configure(test ->
                                            test.getJavaLauncher().set(javaToolchainService.launcherFor(config -> {
                                                config.getLanguageVersion().set(JavaLanguageVersion.of(17));
                                                config.getVendor().set(JvmVendorSpec.AZUL);
                                            })))));
            suites.configureEach(suite -> {
                if (suite instanceof JvmTestSuite) {
                    var jvmTestSuite = (JvmTestSuite) suite;
                    jvmTestSuite.useJUnitJupiter();
                    jvmTestSuite.dependencies(jvmComponentDependencies -> {
                        var implementation = jvmComponentDependencies.getImplementation();
                        implementation.add(jvmComponentDependencies.project());
                    });
                    jvmTestSuite.getTargets().all(target -> {
                        target.getTestTask().configure(test -> {
                            var sysProps = new HashMap<>(test.getSystemProperties());
                            sysProps.putAll(
                                    Map.of(
                                            "junit.jupiter.execution.parallel.enabled", "true",
                                            "junit.jupiter.execution.parallel.mode.default", "SAME_THREAD"
                                    )
                            );
                            test.setSystemProperties(sysProps);
                            test.getOutputs().upToDateWhen(task -> false);
                            test.testLogging(logging -> {
                                logging.setShowStandardStreams(true);
                            });
                            test.usesService(syncBuildService);
                            test.doFirst(new ConfigureOnBeforeTestStart(syncBuildService));
                            var environment = new HashMap<>(test.getEnvironment());
                            environment.put("TESTCONTAINERS_REUSE_ENABLE", "true");
                            test.setEnvironment(environment);
                            test.setJvmArgs(
                                    Stream.of(
                                                    test.getJvmArgs(),
                                                    Arrays.asList(
                                                            "-XX:TieredStopAtLevel=1",
                                                            "-noverify",
//                                                            "-Xmx2048m",
                                                            "-XX:+UseParallelGC",
                                                            "-XX:ParallelGCThreads=2"
                                                    )
                                            )
                                            .filter(Objects::nonNull)
                                            .flatMap(Collection::stream)
                                            .collect(Collectors.toList())
                            );
                        });
                    });
                }
            });
            tasks.named("check", task -> task.dependsOn(integrationTestSuite));
            tasks.register("fastTests", Test.class, test -> {
                test.setGroup("verification");
                test.setDescription("Runs only fast tests, which includes unit tests and some integration tests");
                test.dependsOn("test");
                if (!integrationTestTaskName.equals("functionalTest")) {
                    test.dependsOn(integrationTestTaskName);
                }
            });
            configurations.named(integrationTestTaskName + "Implementation", configuration -> {
                configuration.extendsFrom(configurations.getByName("testImplementation"));
            });
            configurations.named(integrationTestTaskName + "AnnotationProcessor", configuration -> {
                configuration.extendsFrom(configurations.getByName("testAnnotationProcessor"));
            });
            configurations.named(integrationTestTaskName + "CompileOnly", configuration -> {
                configuration.extendsFrom(configurations.getByName("testCompileOnly"));
            });
            configurations.named(integrationTestTaskName + "RuntimeOnly", configuration -> {
                configuration.extendsFrom(configurations.getByName("testRuntimeOnly"));
            });
        });
    }
}
