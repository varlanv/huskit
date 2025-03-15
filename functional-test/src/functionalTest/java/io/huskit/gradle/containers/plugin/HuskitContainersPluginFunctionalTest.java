package io.huskit.gradle.containers.plugin;

import io.huskit.common.HtConstants;
import io.huskit.gradle.DockerFunctionalTest;
import io.huskit.gradle.GradleRunResult;
import io.huskit.gradle.commontest.DataTable;
import io.huskit.gradle.containers.plugin.internal.HuskitContainersExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
public class HuskitContainersPluginFunctionalTest implements DockerFunctionalTest {

    @ParameterizedTest
    @MethodSource("defaultDataTables")
    @DisplayName("should add 'serviceContainers' extension")
    void should_add_servicecontainers_extension(DataTable dataTable) {
        runGradleRunnerFixture(
            dataTable,
            List.of("help"),
            fixture -> {
                setFileText(fixture.rootBuildFile(), "plugins { id 'io.huskit.gradle.containers-plugin' }");
                var buildResult = build(fixture.runner());
                assertThat(buildResult.getOutput()).contains(HuskitContainersExtension.name());
            });
    }

    @ParameterizedTest
    @MethodSource("defaultDataTables")
    @DisplayName("apply-plugin-to-single-java-project should work correctly")
    void apply_plugin_to_single_java_project_should_work_correctly(DataTable dataTable) {
        var useCaseName = "apply-plugin-to-single-java-project-gradle8";
        runUseCaseFixture(
            useCaseName,
            dataTable,
            result -> {
                var messages = result.findMarkedMessages(HtConstants.Mongo.DEFAULT_CONNECTION_STRING_ENV).values();
                assertThat(messages.size()).isEqualTo(2);
                assertThat(Set.copyOf(messages).size()).isEqualTo(2);
                assertThat(findHuskitContainersForUseCase(useCaseName).size()).isEqualTo(0);
            });
    }

    @ParameterizedTest
    @MethodSource("defaultDataTables")
    @DisplayName("apply-plugin-to-multiple-java-projects-all-not-reusable should work correctly")
    void apply_plugin_to_multiple_java_projects_all_not_reusable_should_work_correctly(DataTable dataTable) {
        var useCaseName = "apply-plugin-to-multiple-java-projects-all-not-reusable-gradle8";
        runUseCaseFixture(
            useCaseName,
            dataTable,
            result -> {
                var messages = result.findMarkedMessages(HtConstants.Mongo.DEFAULT_CONNECTION_STRING_ENV).values();
                assertThat(messages.size()).isEqualTo(6);
                assertThat(Set.copyOf(messages).size()).isEqualTo(6);
                assertThat(findHuskitContainersForUseCase(useCaseName).size()).isEqualTo(0);
            });
    }

    @ParameterizedTest
    @MethodSource("defaultDataTables")
    @DisplayName("apply-plugin-to-multiple-java-projects all reusable should work correctly")
    void apply_plugin_to_multiple_java_projects_all_reusable_should_work_correctly(DataTable dataTable) {
        var useCaseName = "apply-plugin-to-multiple-java-projects-gradle8";
        runUseCaseFixture(
            useCaseName,
            dataTable,
            result -> {
                var messages = result.findMarkedMessages(HtConstants.Mongo.DEFAULT_CONNECTION_STRING_ENV).values();

                // Mongo containers were requested 6 times while only 1 unique connection string is used
                assertThat(messages.size()).isEqualTo(6);
                var mongoHosts = messages.stream()
                    .map(it -> StringUtils.substringBefore(StringUtils.substringAfter(it, "mongodb://"), "/"))
                    .collect(Collectors.toList());
                assertThat(Set.copyOf(mongoHosts)).hasSize(1);

                // Mongo container is still available
                var containers = findHuskitContainersForUseCase(useCaseName);
                assertThat(containers).hasSize(1);
            }
        );
    }

    @ParameterizedTest
    @MethodSource("defaultDataTables")
    @DisplayName("apply-plugin-to-multiple-java-projects-with-and-without-reuse should work correctly")
    void apply_plugin_to_multiple_java_projects_with_and_without_reuse_should_work_correctly(DataTable dataTable) {
        var useCaseName = "apply-plugin-to-multiple-java-projects-with-and-without-reuse-gradle8";
        runUseCaseFixture(
            useCaseName,
            dataTable,
            result -> {
                var messages = result.findMarkedMessages(HtConstants.Mongo.DEFAULT_CONNECTION_STRING_ENV).values();

                // mongo containers were requested 6 times
                assertThat(messages).hasSize(6);
                var mongoHosts = messages.stream()
                    .map(it -> StringUtils.substringBefore(StringUtils.substringAfter(it, "mongodb://"), "/"))
                    .collect(Collectors.toList());

                // 3 unique connection strings are used - one reusable and two non-reusable(one for each request)
                assertThat(Set.copyOf(mongoHosts)).hasSize(3);

                // reusable mongo container is still available
                var containers = findHuskitContainersForUseCase(useCaseName);
                assertThat(containers).hasSize(1);
            });
    }

    private void runUseCaseFixture(String useCaseName, DataTable dataTable, ThrowingConsumer<GradleRunResult> fixtureConsumer) {
        runGradleRunnerFixture(
            dataTable,
            List.of("check"),
            fixture -> {
                var useCaseDir = useCaseDir(useCaseName);
                assertThat(useCaseDir).isNotNull();
                assertThat(useCaseDir.exists()).isTrue();
                assertThat(useCaseDir.isDirectory()).isTrue();
                copyFolderContents(useCaseDir, fixture.subjectProjectDir());
                copyFolderContents(useCasesCommonLogicDir(), fixture.rootTestProjectDir());
                var env = new HashMap<>(Map.of("FUNCTIONAL_SPEC_RUN", "true"));
                if (fixture.runner().getEnvironment() != null) {
                    env.putAll(fixture.runner().getEnvironment());
                }
                var runner = fixture.runner().withEnvironment(env);
                var build1result = build(runner);
                var build2result = build(runner);
                fixtureConsumer.accept(new GradleRunResult(new ArrayList<>(List.of(build1result, build2result))));
            }
        );
    }

    private File useCaseDir(String useCaseDirName) {
        return huskitProjectRoot().toPath().resolve(Paths.get("use-cases", "plugins", "containers-plugin", useCaseDirName)).toFile();
    }

    private File useCasesCommonLogicDir() {
        return huskitProjectRoot().toPath().resolve(Paths.get("use-cases", "common-use-cases-logic")).toFile();
    }
}
