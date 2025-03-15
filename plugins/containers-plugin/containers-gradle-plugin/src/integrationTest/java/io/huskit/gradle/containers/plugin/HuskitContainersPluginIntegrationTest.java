package io.huskit.gradle.containers.plugin;

import io.huskit.common.Sneaky;
import io.huskit.containers.integration.DfHtStartedContainer;
import io.huskit.containers.integration.FakeHtIntegratedDocker;
import io.huskit.common.HtConstants;
import io.huskit.containers.model.exception.NonUniqueContainerException;
import io.huskit.gradle.commontest.GradleIntegrationTest;
import io.huskit.gradle.containers.plugin.api.ContainersExtension;
import io.huskit.gradle.containers.plugin.internal.AddContainersEnvironment;
import io.huskit.gradle.containers.plugin.internal.ContainersBuildServiceParams;
import io.huskit.gradle.containers.plugin.internal.ContainersTask;
import io.huskit.gradle.containers.plugin.internal.HuskitContainersExtension;
import io.huskit.gradle.containers.plugin.internal.buildservice.ContainersBuildService;
import io.huskit.gradle.containers.plugin.internal.spec.mongo.MongoContainerRequestSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.services.BuildServiceRegistration;
import org.gradle.api.tasks.TaskProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HuskitContainersPluginIntegrationTest implements GradleIntegrationTest {

    Integer anyFixedPort = 21213;
    Integer anyHostPort = 21242;
    String anyConnectionString = "anyConnectionString";
    String anyDbName = "anyDbName";
    String anyImage = "anyImage";
    String anyExposedPortEnv = "anyExposedPortEnv";
    String anyExposedDbNameEnv = "anyExposedDbNameEnv";
    String anyExposedConnectionStringEnv = "anyExposedConnectionStringEnv";

    @Test
    @DisplayName("Should create build service")
    void should_create_build_service() {
        runProjectFixture(fixture -> {
            // GIVEN
            var project = fixture.project();

            // WHEN
            project.getPlugins().apply(HuskitContainersPlugin.class);
            evaluateProject(project);

            // THEN
            gradleAssert(project).hasOnlyOneService(ContainersBuildService.name());
        });
    }

    @Test
    @DisplayName("If build service already added and plugin applied, then no error thrown")
    void if_build_service_already_added_and_plugin_applied_then_no_error_thrown() {
        runProjectFixture(fixture -> {
            // GIVEN
            var project = fixture.project();
            project.getGradle().getSharedServices().registerIfAbsent(
                    ContainersBuildService.name(),
                    ContainersBuildService.class,
                    params -> {
                    }
            );

            // WHEN
            project.getPlugins().apply(HuskitContainersPlugin.class);

            // THEN
            gradleAssert(project).hasOnlyOneService(ContainersBuildService.name());
        });
    }

    @Test
    @DisplayName("If parent project already created build service and plugin applied, then no error thrown")
    void if_parent_project_already_created_build_service_and_plugin_applied_then_no_error_thrown() {
        runProjectWithParentFixture(fixture -> {
            // WHEN
            fixture.parentProject().getPlugins().apply(HuskitContainersPlugin.class);
            fixture.project().getPlugins().apply(HuskitContainersPlugin.class);
            evaluateProject(fixture.parentProject());
            evaluateProject(fixture.project());

            // THEN
            gradleAssert(fixture.project()).hasOnlyOneService(ContainersBuildService.name());
            gradleAssert(fixture.parentProject()).hasOnlyOneService(ContainersBuildService.name());
        });
    }

    @Test
    @DisplayName("When build service evaluated, no exception thrown")
    void when_build_service_evaluated_no_exception_thrown() {
        runProjectFixture(fixture -> {
            // GIVEN
            var project = fixture.project();
            project.getPlugins().apply(HuskitContainersPlugin.class);
            evaluateProject(project);

            // WHEN
            var buildServiceProvider = List.copyOf(
                    project.getGradle().getSharedServices().getRegistrations()
            ).get(0).getService();

            // THEN
            assertThat(buildServiceProvider.isPresent()).isTrue();
        });
    }

    @Test
    @DisplayName("If there is one project with plugin, then max parallel uses should be 1")
    void if_there_is_one_project_with_plugin_then_max_parallel_uses_should_be_1() {
        runProjectFixture(fixture -> {
            // GIVEN
            var project = fixture.project();

            // WHEN
            project.getPlugins().apply(HuskitContainersPlugin.class);
            evaluateProject(project);

            // THEN
            gradleAssert(project).withBuildServiceRegistration(ContainersBuildService.name(), registration -> {
                assertThat(registration.getMaxParallelUsages().isPresent()).isTrue();
                assertThat(registration.getMaxParallelUsages().get()).isEqualTo(1);
            });
        });
    }

    @Test
    @DisplayName("If there are two projects with plugin, then max parallel uses should be 2")
    void if_there_are_two_projects_with_plugin_then_max_parallel_uses_should_be_2() {
        runMultiProjectContainerFixture(fixture -> {
            // EXPECT
            assertThat(fixture.maxParallelUsages.get()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("When applying plugin, then should create extension")
    void when_applying_plugin_then_should_create_extension() {
        runProjectFixture(fixture -> {
            // GIVEN
            var project = fixture.project();

            // WHEN
            project.getPlugins().apply(HuskitContainersPlugin.class);
            evaluateProject(project);

            // THEN
            gradleAssert(project)
                    .hasExtensionWithName(HuskitContainersExtension.name())
                    .hasExtensionWithType(ContainersExtension.class);
        });
    }

    @Test
    @DisplayName("Adding mongo container with database name should set it")
    void adding_mongo_container_with_database_name_should_set_it() {
        runProjectFixture(fixture -> {
            // GIVEN
            var project = fixture.project();

            // WHEN
            project.getPlugins().apply(HuskitContainersPlugin.class);
            var containersExtension = (HuskitContainersExtension) project.getExtensions().getByType(ContainersExtension.class);
            containersExtension.mongo(mongo -> mongo.databaseName(anyDbName));

            // THEN
            var requestedContainers = containersExtension.getContainersRequestedByUser().get();
            assertThat(requestedContainers).hasSize(1);
            var requestedContainer = (MongoContainerRequestSpec) requestedContainers.get(0);
            assertThat(requestedContainer.getDatabaseName().get()).isEqualTo(anyDbName);
        });
    }

    @Test
    @DisplayName("Adding mongo container with image should set it")
    void adding_mongo_container_with_image_should_set_it() {
        runProjectFixture(fixture -> {
            // GIVEN
            var project = fixture.project();

            // WHEN
            project.getPlugins().apply(HuskitContainersPlugin.class);
            var containersExtension = (HuskitContainersExtension) project.getExtensions().getByType(ContainersExtension.class);
            containersExtension.mongo(mongo -> mongo.image(anyImage));

            // THEN
            var requestedContainers = containersExtension.getContainersRequestedByUser().get();
            assertThat(requestedContainers).hasSize(1);
            var requestedContainer = (MongoContainerRequestSpec) requestedContainers.get(0);
            assertThat(requestedContainer.getImage().get()).isEqualTo(anyImage);
        });
    }

    @Test
    @DisplayName("Adding mongo container with fixed port should set it")
    void adding_mongo_container_with_fixed_port_should_set_it() {
        runProjectFixture(fixture -> {
            // GIVEN
            var project = fixture.project();

            // WHEN
            project.getPlugins().apply(HuskitContainersPlugin.class);
            var containersExtension = (HuskitContainersExtension) project.getExtensions().getByType(ContainersExtension.class);
            containersExtension.mongo(mongo -> mongo.port(portSpec -> portSpec.fixed(fixedPortSpec -> {
                fixedPortSpec.containerValue(HtConstants.Mongo.DEFAULT_PORT);
                fixedPortSpec.hostValue(anyHostPort);
            })));

            // THEN
            var requestedContainers = containersExtension.getContainersRequestedByUser().get();
            assertThat(requestedContainers).hasSize(1);
            var requestedContainer = (MongoContainerRequestSpec) requestedContainers.get(0);
            var portSpec = requestedContainer.getPort().get();
            var fixedPortSpec = portSpec.getFixed().get();
            assertThat(fixedPortSpec.getHostValue().get()).isEqualTo(anyHostPort);
            assertThat(fixedPortSpec.getContainerValue().get()).isEqualTo(HtConstants.Mongo.DEFAULT_PORT);
            assertThat(fixedPortSpec.getHostRange().isPresent()).isFalse();
            assertThat(portSpec.getDynamic().get()).isFalse();
        });
    }

    @Test
    @DisplayName("If no containers added, then extension-requested container list is empty")
    void if_no_containers_added_then_extension_requested_container_list_is_empty() {
        runProjectFixture(fixture -> {
            // GIVEN
            var project = fixture.project();

            // WHEN
            project.getPlugins().apply(HuskitContainersPlugin.class);

            // THEN
            var containersExtension = (HuskitContainersExtension) project.getExtensions().getByType(ContainersExtension.class);
            var requestedContainers = containersExtension.getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @Test
    @DisplayName("When containers task set to start before test (string) task, then should add 'dependsOn' to test task")
    void when_containers_task_set_to_start_before_test_string_task_then_should_add_dependson_to_test_task() {
        runSingleProjectContainerFixture(fixture -> {
            // GIVEN
            var project = fixture.project();

            // WHEN
            fixture.containersExtension().shouldStartBefore(spec -> spec.task(JavaPlugin.TEST_TASK_NAME));
            evaluateProject(project);

            // THEN
            gradleAssert(fixture.testTaskProvider())
                    .dependsOn(fixture.containersTask())
                    .mustRunAfter(fixture.containersTask())
                    .requiresService(fixture.dockerBuildServiceProvider());
        });
    }

    @Test
    @DisplayName("When containers task set to start before test (Task) task, then should add 'dependsOn' to test task")
    void when_containers_task_set_to_start_before_test_task_task_then_should_add_dependson_to_test_task() {
        runSingleProjectContainerFixture(fixture -> {
            // GIVEN
            var project = fixture.project();

            // WHEN
            fixture.containersExtension().shouldStartBefore(spec -> spec.task(project.getTasks().getByName(JavaPlugin.TEST_TASK_NAME)));
            evaluateProject(project);

            // THEN
            gradleAssert(fixture.testTaskProvider())
                    .dependsOn(fixture.containersTask())
                    .mustRunAfter(fixture.containersTask())
                    .requiresService(fixture.dockerBuildServiceProvider());
        });
    }

    @Test
    @DisplayName("When containers task set to start before test (TaskProvider) task, then should add 'dependsOn' to test task")
    void when_containers_task_set_to_start_before_test_taskprovider_task_then_should_add_dependson_to_test_task() {
        runSingleProjectContainerFixture(fixture -> {
            // GIVEN
            var project = fixture.project();

            // WHEN
            fixture.containersExtension().shouldStartBefore(spec -> spec.task(project.getTasks().named(JavaPlugin.TEST_TASK_NAME)));
            evaluateProject(project);

            // THEN
            gradleAssert(fixture.testTaskProvider())
                    .dependsOn(fixture.containersTask())
                    .mustRunAfter(fixture.containersTask())
                    .requiresService(fixture.dockerBuildServiceProvider());
        });
    }

    @Test
    @DisplayName("When 'start before' is not configured, containers task is not created")
    void when_start_before_is_not_configured_containers_task_is_not_created() {
        runSingleProjectContainerFixture(fixture -> {
            // WHEN
            var project = fixture.project();
            evaluateProject(project);

            // THEN
            gradleAssert(project).doesNotHaveTask(ContainersTask.nameForTask(JavaPlugin.TEST_TASK_NAME));
            assertThat(fixture.integratedDocker().receivedSpecs().isEmpty());
        });
    }

    @Test
    @DisplayName("When 'start before' is configured but no containers added, containers task doesn't start any containers")
    void when_start_before_is_configured_but_no_containers_added_containers_task_doesn_t_start_any_containers() {
        runSingleProjectContainerFixture(fixture -> {
            // GIVEN
            var project = fixture.project();

            // WHEN
            fixture.containersExtension().shouldStartBefore(spec -> spec.task(JavaPlugin.TEST_TASK_NAME));
            evaluateProject(project);

            // THEN
            assertThat(fixture.containersTask().get().startAndReturnContainers()).isEmpty();
        });
    }

    @Test
    @DisplayName("When empty mongo spec provided, use default values to start mongo container")
    void when_empty_mongo_spec_provided_use_default_values_to_start_mongo_container() {
        runSingleProjectContainerFixture(fixture -> {
            // GIVEN
            var project = fixture.project();
            fixture.containersExtension().shouldStartBefore(spec -> spec.task(JavaPlugin.TEST_TASK_NAME));
            fixture.containersExtension().mongo(mongoSpec -> {
            });
            evaluateProject(project);
            var env = Map.of(
                    HtConstants.Mongo.DEFAULT_CONNECTION_STRING_ENV, anyConnectionString,
                    HtConstants.Mongo.DEFAULT_PORT_ENV, String.valueOf(anyFixedPort),
                    HtConstants.Mongo.DEFAULT_DB_NAME_ENV, HtConstants.Mongo.DEFAULT_DB_NAME
            );
            fixture.integratedDocker().addContainer(new DfHtStartedContainer("mongo", "hash", env));

            // WHEN
            fixture.containersTask().get().startAndReturnContainers(fixture.integratedDocker());
            var addContainersEnvironmentAction = fixture.getAddContainersEnvironmentAction();
            var environment = addContainersEnvironmentAction.executeAndReturn(
                    fixture.testTaskProvider().get(),
                    fixture.integratedDocker()
            );

            // THEN
            assertThat(environment).isNotEmpty();
            assertThat(environment).containsExactlyEntriesOf(env);
        });
    }

    @Test
    @DisplayName("When mongo spec provided with non-default exposed environment values then should use them")
    void when_mongo_spec_provided_with_non_default_exposed_environment_values_then_should_use_them() {
        runSingleProjectContainerFixture(fixture -> {
            // GIVEN
            var project = fixture.project();
            fixture.containersExtension().shouldStartBefore(spec -> spec.task(JavaPlugin.TEST_TASK_NAME));
            fixture.containersExtension().mongo(mongoSpec -> {
                mongoSpec.exposedEnvironment(exposedEnvSpec -> {
                    exposedEnvSpec.connectionString(anyExposedConnectionStringEnv);
                    exposedEnvSpec.databaseName(anyExposedDbNameEnv);
                    exposedEnvSpec.port(anyExposedPortEnv);
                });
            });
            evaluateProject(project);
            var env = Map.of(
                    anyExposedConnectionStringEnv, anyConnectionString,
                    anyExposedPortEnv, String.valueOf(anyFixedPort),
                    anyExposedDbNameEnv, HtConstants.Mongo.DEFAULT_DB_NAME
            );
            fixture.integratedDocker().addContainer(new DfHtStartedContainer("mongo", "hash", env));

            // WHEN
            fixture.containersTask().get().startAndReturnContainers(fixture.integratedDocker());
            var addContainersEnvironmentAction = fixture.getAddContainersEnvironmentAction();
            var environment = addContainersEnvironmentAction.executeAndReturn(
                    fixture.testTaskProvider().get(),
                    fixture.integratedDocker()
            );

            // THEN
            assertThat(environment).isNotEmpty();
            assertThat(environment).containsExactlyEntriesOf(env);
        });
    }

    @Test
    @DisplayName("When mongo spec has two duplicate containers, then should throw exception")
    void when_mongo_spec_has_two_duplicate_containers_then_should_throw_exception() {
        runSingleProjectContainerFixture(fixture -> {
            // WHEN
            fixture.containersExtension().shouldStartBefore(spec -> spec.task(JavaPlugin.TEST_TASK_NAME));
            fixture.containersExtension().mongo(mongoSpec -> {
            });

            // THEN
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongoSpec -> {
            })).isInstanceOf(NonUniqueContainerException.class);
        });
    }

    @Test
    @DisplayName("Mongo container when `containerValue` is not set, then should use default mongo port")
    void mongo_container_when_containervalue_is_not_set_then_should_use_default_mongo_port() {
        runSingleProjectContainerFixture(fixture -> {
            // GIVEN
            fixture.containersExtension().mongo(mongo ->
                    mongo.port(portSpec ->
                            portSpec.fixed(fixedPortSpec ->
                                    fixedPortSpec.hostValue(anyHostPort))));

            // EXPECT
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).hasSize(1);
            var requestedContainer = (MongoContainerRequestSpec) requestedContainers.get(0);
            var portSpec = requestedContainer.getPort().get();
            var fixedPortSpec = portSpec.getFixed().get();
            assertThat(fixedPortSpec.getHostValue().get()).isEqualTo(anyHostPort);
            assertThat(fixedPortSpec.getContainerValue().get()).isEqualTo(HtConstants.Mongo.DEFAULT_PORT);
            assertThat(fixedPortSpec.getHostRange().isPresent()).isFalse();
            assertThat(portSpec.getDynamic().getOrNull()).isFalse();
        });
    }

    @Test
    @DisplayName("Mongo container when `fixed` port is not set, then use dynamic port")
    void mongo_container_when_fixed_port_is_not_set_then_use_dynamic_port() {
        runSingleProjectContainerFixture(fixture -> {
            // GIVEN
            fixture.containersExtension().mongo(mongo -> {
            });

            // EXPECT
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).hasSize(1);
            var requestedContainer = (MongoContainerRequestSpec) requestedContainers.get(0);
            var portSpec = requestedContainer.getPort().get();
            var fixedPortSpec = portSpec.getFixed().get();
            assertThat(fixedPortSpec.getHostValue().getOrNull()).isNull();
            assertThat(fixedPortSpec.getContainerValue().getOrNull()).isEqualTo(HtConstants.Mongo.DEFAULT_PORT);
            assertThat(fixedPortSpec.getHostRange().isPresent()).isFalse();
            assertThat(portSpec.getDynamic().getOrNull()).isTrue();
        });
    }

    @Test
    @DisplayName("Mongo container when `fixed` port spec is empty, then should throw exception")
    void mongo_container_when_fixed_port_spec_is_empty_then_should_throw_exception() {
        runSingleProjectContainerFixture(fixture -> {
            // EXPECT
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongo ->
                    mongo.port(portSpec ->
                            portSpec.fixed(fixedPortSpec -> {
                            }))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Fixed port must be set to");

            // AND
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @Test
    @DisplayName("Mongo container when already set `hostRange` and trying to set `hostValue`, then should throw exception")
    void mongo_container_when_already_set_hostrange_and_trying_to_set_hostvalue_then_should_throw_exception() {
        runSingleProjectContainerFixture(fixture -> {
            // EXPECT
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongo ->
                    mongo.port(portSpec ->
                            portSpec.fixed(fixedPortSpec -> {
                                fixedPortSpec.hostRange(1, 2);
                                fixedPortSpec.hostValue(42);
                            }))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("42")
                    .hasMessageContaining("[1, 2]")
                    .hasMessageContaining("Can't set port");

            // AND
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @Test
    @DisplayName("Mongo container when `fixed` port is already set to `hostValue` and try to set `hostRange` then should throw exception")
    void mongo_container_when_fixed_port_is_already_set_to_hostvalue_and_try_to_set_hostrange_then_should_throw_exception() {
        runSingleProjectContainerFixture(fixture -> {
            // EXPECT
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongo ->
                    mongo.port(portSpec ->
                            portSpec.fixed(fixedPortSpec -> {
                                fixedPortSpec.hostValue(42);
                                fixedPortSpec.hostRange(1, 2);
                            }))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("42")
                    .hasMessageContaining("[1, 2]")
                    .hasMessageContaining("Can't set port");

            // AND
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -42})
    @DisplayName("Mongo container when `fixed` port is set to negative `hostValue`, should throw exception")
    void mongo_container_when_fixed_port_is_set_to_negative_hostvalue_should_throw_exception(Integer hostValue) {
        runSingleProjectContainerFixture(fixture -> {
            // EXPECT
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongo ->
                    mongo.port(portSpec ->
                            portSpec.fixed(fixedPortSpec -> {
                                fixedPortSpec.hostValue(hostValue);
                            }))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(hostValue.toString())
                    .hasMessageContaining("must be greater than 0");

            // AND
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -42})
    @DisplayName("Mongo container when `fixed` port is set to negative `containerValue`, should throw exception")
    void mongo_container_when_fixed_port_is_set_to_negative_containervalue_should_throw_exception(Integer containerValue) {
        runSingleProjectContainerFixture(fixture -> {
            // EXPECT
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongo ->
                    mongo.port(portSpec ->
                            portSpec.fixed(fixedPortSpec -> {
                                fixedPortSpec.hostValue(1);
                                fixedPortSpec.containerValue(containerValue);
                            }))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(containerValue.toString())
                    .hasMessageContaining("must be greater than 0");

            // AND
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @ParameterizedTest
    @CsvSource({
            "0, 1",
            "1, 0",
            "0, 0",
            "-1, 1",
            "1, -1",
            "-1, -1"
    })
    @DisplayName("Mongo container when negative `hostPortFrom` or `hostPortTo`, should throw exception")
    void mongo_container_when_negative_hostportfrom_or_hostportto_should_throw_exception(Integer hostPortFrom, Integer hostPortTo) {
        runSingleProjectContainerFixture(fixture -> {
            // EXPECT
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongo ->
                    mongo.port(portSpec ->
                            portSpec.fixed(fixedPortSpec ->
                                    fixedPortSpec.hostRange(hostPortFrom, hostPortTo)))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Fixed port range %s", List.of(hostPortFrom, hostPortTo))
                    .hasMessageContaining("must be greater than 0");

            // AND
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 6})
    @DisplayName("Mongo container when `hostPortFrom` is equal to or greater than `hostPortTo`, should throw exception")
    void mongo_container_when_hostportfrom_is_equal_to_or_greater_than_hostportto_should_throw_exception(int hostPortFrom) {
        runSingleProjectContainerFixture(fixture -> {
            var hostPortTo = 5;
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongo ->
                    mongo.port(portSpec ->
                            portSpec.fixed(fixedPortSpec ->
                                    fixedPortSpec.hostRange(hostPortFrom, hostPortTo)))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be less than range end [%s]", hostPortTo);

            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @Test
    @DisplayName("When mongo `cleanupSpec` time unit is unknown, then should throw exception")
    void when_mongo_cleanupspec_time_unit_is_unknown_then_should_throw_exception() {
        runSingleProjectContainerFixture(fixture -> {
            // EXPECT
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongoSpec ->
                    mongoSpec.reuse(reuseSpec ->
                            reuseSpec.cleanup(cleanupSpec -> cleanupSpec.after(1, "sec")))))
                    .hasMessageContaining("Unavailable unit value - 'sec'");

            // AND
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @Test
    @DisplayName("When mongo `cleanupSpec` when known time unit is provided, should configure cleanup")
    void when_mongo_cleanupspec_when_known_time_unit_is_provided_should_configure_cleanup() {
        runSingleProjectContainerFixture(fixture -> {
            // WHEN
            fixture.containersExtension().mongo(mongoSpec ->
                    mongoSpec.reuse(reuseSpec ->
                            reuseSpec.cleanup(cleanupSpec -> cleanupSpec.after(1, "hours"))));

            // THEN
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).hasSize(1);
            var containerSpec = (MongoContainerRequestSpec) requestedContainers.get(0);
            var reuseSpec = containerSpec.getReuse().get();
            var cleanupSpec = reuseSpec.getCleanupSpec().get();
            assertThat(cleanupSpec.getCleanupAfter().get()).isEqualTo(Duration.ofHours(1));
        });
    }

    @Test
    @DisplayName("When mongo `cleanupSpec` time unit is not allowed, then should throw exception")
    void when_mongo_cleanupspec_time_unit_is_not_allowed_then_should_throw_exception() {
        runSingleProjectContainerFixture(fixture -> {
            // EXPECT
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongoSpec ->
                    mongoSpec.reuse(reuseSpec ->
                            reuseSpec.cleanup(cleanupSpec -> cleanupSpec.after(1, ChronoUnit.DECADES)))))
                    .hasMessageContaining("Unavailable unit value - '%s'", ChronoUnit.DECADES);

            // AND
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @Test
    @DisplayName("When mongo `cleanupSpec` negative time value is provided, then should throw exception")
    void when_mongo_cleanupspec_negative_time_value_is_provided_then_should_throw_exception() {
        runSingleProjectContainerFixture(fixture -> {
            // EXPECT
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongoSpec ->
                    mongoSpec.reuse(reuseSpec ->
                            reuseSpec.cleanup(cleanupSpec -> cleanupSpec.after(-1, ChronoUnit.DAYS)))))
                    .hasMessageContaining("`cleanupAfter` [%s] cannot be negative", Duration.ofDays(-1));

            // AND
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @Test
    @DisplayName("When mongo `cleanupSpec` time value less than 60 seconds is provided, then should throw exception")
    void when_mongo_cleanupspec_time_value_less_than_60_seconds_is_provided_then_should_throw_exception() {
        runSingleProjectContainerFixture(fixture -> {
            // EXPECT
            assertThatThrownBy(() -> fixture.containersExtension().mongo(mongoSpec ->
                    mongoSpec.reuse(reuseSpec ->
                            reuseSpec.cleanup(cleanupSpec -> cleanupSpec.after(59, ChronoUnit.SECONDS)))))
                    .hasMessageContaining("cannot be less than 60 seconds");

            // AND
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    @Test
    @DisplayName("When mongo `cleanupSpec` 0 time value is provided, should consider it as 'forever'")
    void when_mongo_cleanupspec_0_time_value_is_provided_should_consider_it_as_forever() {
        runSingleProjectContainerFixture(fixture -> {
            // WHEN
            fixture.containersExtension().mongo(mongoSpec ->
                    mongoSpec.reuse(reuseSpec ->
                            reuseSpec.cleanup(cleanupSpec -> cleanupSpec.after(0, ChronoUnit.SECONDS))));

            // THEN
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).hasSize(1);
            var containerSpec = (MongoContainerRequestSpec) requestedContainers.get(0);
            var reuseSpec = containerSpec.getReuse().get();
            var cleanupSpec = reuseSpec.getCleanupSpec().get();
            assertThat(cleanupSpec.getCleanupAfter().get()).isEqualTo(Duration.ZERO);
        });
    }

    @Test
    @DisplayName("When mongo `cleanupSpec` Duration object is provided, then sould convert it to millis")
    void when_mongo_cleanupspec_duration_object_is_provided_then_sould_convert_it_to_millis() {
        runSingleProjectContainerFixture(fixture -> {
            // WHEN
            fixture.containersExtension().mongo(mongoSpec ->
                    mongoSpec.reuse(reuseSpec ->
                            reuseSpec.cleanup(cleanupSpec -> cleanupSpec.after(Duration.ofSeconds(100)))));

            // THEN
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).hasSize(1);
            var containerSpec = (MongoContainerRequestSpec) requestedContainers.get(0);
            var reuseSpec = containerSpec.getReuse().get();
            var cleanupSpec = reuseSpec.getCleanupSpec().get();
            assertThat(cleanupSpec.getCleanupAfter().get()).isEqualTo(Duration.ofSeconds(100));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("When mongo `reuseSpec` enabled value is provided, then should configure reuse")
    void when_mongo_reusespec_enabled_value_is_provided_then_should_configure_reuse(boolean reuseEnabled) {
        runSingleProjectContainerFixture(fixture -> {
            // WHEN
            fixture.containersExtension().mongo(mongoSpec ->
                    mongoSpec.reuse(reuseSpec -> reuseSpec.enabled(reuseEnabled)));

            // THEN
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).hasSize(1);
            var containerSpec = (MongoContainerRequestSpec) requestedContainers.get(0);
            var reuseSpec = containerSpec.getReuse().get();
            assertThat(reuseSpec.getEnabled().get()).isEqualTo(reuseEnabled);
        });
    }

    @Test
    @DisplayName("When mongo `shouldStartBefore` spec is already set and trying to set it again, then should throw exception")
    void when_mongo_shouldstartbefore_spec_is_already_set_and_trying_to_set_it_again_then_should_throw_exception() {
        runSingleProjectContainerFixture(fixture -> {
            // EXPECT
            assertThatThrownBy(() -> fixture.containersExtension().shouldStartBefore(shouldStartBeforeSpec -> {
                shouldStartBeforeSpec.task(BasePlugin.CLEAN_TASK_NAME);
                shouldStartBeforeSpec.task(JavaPlugin.TEST_TASK_NAME);
            })).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already been set")
                    .hasMessageContaining("shouldRunBefore");

            // AND
            var requestedContainers = fixture.containersExtension().getContainersRequestedByUser().get();
            assertThat(requestedContainers).isEmpty();
        });
    }

    protected static AddContainersEnvironment findTaskAction(Task task, Class<AddContainersEnvironment> type) {
        return ((DefaultTask) task).getTaskActions().stream()
                .map(actionWrapper -> {
                    try {
                        for (var field : actionWrapper.getClass().getDeclaredFields()) {
                            if ("action".equals(field.getName())) {
                                field.setAccessible(true);
                                var action = field.get(actionWrapper);
                                if (action.getClass().equals(type)) {
                                    return (AddContainersEnvironment) field.get(actionWrapper);
                                }
                            }
                        }
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(
                                "No action of type [%s] found in task [%s]",
                                type, task.getName())));
    }

    @SuppressWarnings("unchecked")
    private void runSingleProjectContainerFixture(ThrowingConsumer<SingleProjectContainerFixture> fixtureConsumer) {
        runProjectFixture(fixture -> {
            var project = fixture.project();
            project.getPlugins().apply(JavaPlugin.class);
            project.getPlugins().apply(HuskitContainersPlugin.class);
            Supplier<List<BuildServiceRegistration<?, ?>>> buildServiceRegistrations = () ->
                    new ArrayList<>(project.getGradle().getSharedServices().getRegistrations());
            Supplier<Provider<ContainersBuildService>> dockerBuildServiceProvider = () ->
                    (Provider<ContainersBuildService>) buildServiceRegistrations.get().get(0).getService();
            Supplier<ContainersBuildService> dockerBuildService = () ->
                    dockerBuildServiceProvider.get().get();
            Supplier<Provider<Integer>> maxParallelUsages = () ->
                    buildServiceRegistrations.get().get(0).getMaxParallelUsages();
            Supplier<ContainersBuildServiceParams> dockerBuildServiceParams = () ->
                    dockerBuildService.get().getParameters();

            fixtureConsumer.accept(
                    new SingleProjectContainerFixture(
                            project,
                            fixture.projectDir(),
                            fixture.objects(),
                            fixture.providers(),
                            dockerBuildService,
                            dockerBuildServiceProvider,
                            maxParallelUsages,
                            dockerBuildServiceParams,
                            new FakeHtIntegratedDocker(),
                            (HuskitContainersExtension) project.getExtensions().getByType(ContainersExtension.class)
                    )
            );
        });
    }

    private void runMultiProjectContainerFixture(ThrowingConsumer<MultiProjectContainerFixture> fixtureConsumer) {
        runProjectWithParentFixture(delegateFixture -> {
            delegateFixture.project().getPlugins().apply(HuskitContainersPlugin.class);
            delegateFixture.parentProject().getPlugins().apply(HuskitContainersPlugin.class);
            evaluateProject(delegateFixture.parentProject());
            evaluateProject(delegateFixture.project());
            var projectBuildServices = new ArrayList<>(delegateFixture.project().getGradle().getSharedServices().getRegistrations());
            assertThat(projectBuildServices).hasSize(1);
            var service = projectBuildServices.get(0).getService();
            var dockerBuildService = (ContainersBuildService) service.get();
            var maxParallelUsages = projectBuildServices.get(0).getMaxParallelUsages();
            var dockerBuildServiceParams = dockerBuildService.getParameters();
            try {
                fixtureConsumer.accept(
                        new MultiProjectContainerFixture(
                                delegateFixture.project(),
                                delegateFixture.parentProject(),
                                delegateFixture.projectDir(),
                                delegateFixture.parentProjectDir(),
                                delegateFixture.objects(),
                                delegateFixture.providers(),
                                maxParallelUsages,
                                dockerBuildService,
                                dockerBuildServiceParams
                        ));
            } catch (Exception e) {
                Sneaky.rethrow(e);
            }
        });
    }

    @Getter
    @RequiredArgsConstructor
    private static class MultiProjectContainerFixture {

        Project project;
        Project parentProject;
        File projectDir;
        File parentProjectDir;
        ObjectFactory objects;
        ProviderFactory providers;
        Provider<Integer> maxParallelUsages;
        ContainersBuildService dockerBuildService;
        ContainersBuildServiceParams dockerBuildServiceParams;
    }

    @Getter
    @RequiredArgsConstructor
    private static class SingleProjectContainerFixture {

        Project project;
        File projectDir;
        ObjectFactory objects;
        ProviderFactory providers;
        Supplier<ContainersBuildService> dockerBuildService;
        Supplier<Provider<ContainersBuildService>> dockerBuildServiceProvider;
        Supplier<Provider<Integer>> maxParallelUsages;
        Supplier<ContainersBuildServiceParams> dockerBuildServiceParams;
        FakeHtIntegratedDocker integratedDocker;
        HuskitContainersExtension containersExtension;

        ContainersBuildService dockerBuildService() {
            return dockerBuildService.get();
        }

        Provider<ContainersBuildService> dockerBuildServiceProvider() {
            return dockerBuildServiceProvider.get();
        }

        Provider<Integer> maxParallelUsages() {
            return maxParallelUsages.get();
        }

        TaskProvider<org.gradle.api.tasks.testing.Test> testTaskProvider() {
            return project.getTasks().named(JavaPlugin.TEST_TASK_NAME, org.gradle.api.tasks.testing.Test.class);
        }

        TaskProvider<ContainersTask> containersTask() {
            return containersTask(JavaPlugin.TEST_TASK_NAME);
        }

        TaskProvider<ContainersTask> containersTask(String dependentTaskName) {
            return project.getTasks().named(ContainersTask.nameForTask(dependentTaskName), ContainersTask.class);
        }

        AddContainersEnvironment getAddContainersEnvironmentAction() {
            return findTaskAction(testTaskProvider().get(), AddContainersEnvironment.class);
        }

        AddContainersEnvironment getAddContainersEnvironmentAction(TaskProvider<DefaultTask> taskTaskProvider) {
            return findTaskAction(taskTaskProvider.get(), AddContainersEnvironment.class);
        }

        AddContainersEnvironment getAddContainersEnvironmentAction(String taskName) {
            return findTaskAction(project.getTasks().named(taskName).get(), AddContainersEnvironment.class);
        }
    }
}
