package io.huskit.containers.http;

import io.huskit.common.Log;
import io.huskit.common.Mutable;
import io.huskit.common.Sneaky;
import io.huskit.common.port.DynamicContainerPort;
import io.huskit.common.port.MappedPort;
import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.HtContainerStatus;
import io.huskit.containers.api.container.logs.LookFor;
import io.huskit.gradle.commontest.DockerImagesStash;
import io.huskit.gradle.commontest.DockerIntegrationTest;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtHttpDckrIntegrationTest implements DockerIntegrationTest {

    Supplier<HtHttpDckr> subjectSupplier = () ->
        new HtHttpDckr()
            .withCleanOnClose(true)
            .withLog(Log.fakeVerbose())
            .withDefaultTimeout(Duration.ofSeconds(5));

    @Nested
    class SameContainer {

        HtHttpDckr subject = subjectSupplier.get();
        Mutable<HtContainer> containerRef = Mutable.of();
        Map<String, String> containerEnv = Map.of(
            "ENV1", "ENVVALUE1",
            "ENV2", "ENVVALUE2"
        );
        Map<String, String> containerLabels = Map.of(
            "LABEL1", "LABELVALUE1",
            "LABEL2", "LABELVALUE2"
        );
        MappedPort mappedPort1 = new MappedPort(new DynamicContainerPort().hostValue(), 80);
        MappedPort mappedPort2 = new MappedPort(new DynamicContainerPort().hostValue(), 8080);

        @BeforeAll
        void setupAll() {
            var container = subject.containers().run(
                    DockerImagesStash.defaultSmall(),
                    spec -> spec
                        .withCommand(
                            DockerImagesStash.smallImageBusyCommand().command(),
                            DockerImagesStash.smallImageBusyCommand().args()
                        )
                        .withEnv(containerEnv)
                        .withLabels(containerLabels)
                        .withPortBindings(
                            Map.of(
                                mappedPort1.host(), mappedPort1.container(),
                                mappedPort2.host(), mappedPort2.container()
                            )
                        )
                        .withLookFor("Hello World 123", Duration.ofSeconds(10))
                )
                .exec();
            containerRef.set(container);
        }

        @AfterAll
        void cleanupAll() {
            Sneaky.tryAll(
                () -> containerRef.ifPresent(
                    container -> subject
                        .containers()
                        .remove(
                            container.id(),
                            spec -> spec.withVolumes().withForce()
                        )
                        .exec()
                ),
                subject::close
            );
        }

        @Test
        @DisplayName("container should have expected properties")
        void container_should_have_expected_properties() {
            var container = containerRef.require();
            assertThat(container.id()).isNotEmpty();
            containerRef.set(container);
            var containerConfig = container.config();
            assertThat(containerConfig.labels()).containsAllEntriesOf(containerLabels);
            assertThat(containerConfig.env()).containsAllEntriesOf(containerEnv);
            assertThat(container.name()).isNotEmpty();
            assertThat(container.ports().get(0)).isIn(mappedPort1, mappedPort2).isNotEqualTo(container.ports().get(1));
            assertThat(container.ports().get(1)).isIn(mappedPort1, mappedPort2);
        }

        @Test
        @DisplayName("logs follow when look for second line should return two lines")
        void logs_follow_when_look_for_second_line_should_return_two_lines() {
            var logs = new ConcurrentLinkedQueue<>();
            var frames = subject.containers().logs(containerRef.require().id())
                .follow()
                .lookFor(
                    LookFor.lineMatching(
                        line -> {
                            logs.add(line);
                            return line.contains("Hello World 123");
                        }
                    )
                );
            assertThat(logs).containsExactly("Hello World 1", "Hello World 123");
            assertThat(frames.list()).hasSize(2);
            assertThat(frames.list().get(0))
                .satisfies(
                    frame -> {
                        assertThat(frame.stringData()).contains("Hello World 1");
                        assertThat(frame.type()).isEqualTo(FrameType.STDOUT);
                    }
                );
            assertThat(frames.list().get(1))
                .satisfies(
                    frame -> {
                        assertThat(frame.stringData()).contains("Hello World 123");
                        assertThat(frame.type()).isEqualTo(FrameType.STDOUT);
                    }
                );
        }

        @Test
        @DisplayName("logs follow when look for first line should return only first line")
        void logs_follow_when_look_for_first_line_should_return_only_first_line() {
            var logs = new ConcurrentLinkedQueue<>();
            var frames = subject.containers().logs(containerRef.require().id())
                .follow()
                .lookFor(
                    LookFor.lineMatching(
                        line -> {
                            logs.add(line);
                            return Objects.equals(line, "Hello World 1");
                        }
                    )
                );
            assertThat(logs).containsExactly("Hello World 1");
            assertThat(frames.list()).hasSize(1);
            assertThat(frames.list().get(0)).satisfies(frame -> {
                assertThat(frame.stringData()).contains("Hello World 1");
                assertThat(frame.type()).isEqualTo(FrameType.STDOUT);
            });
        }

        @Test
        @DisplayName("logs should return expected lines")
        void logs_should_return_expected_lines() {
            var frames = subject
                .containers()
                .logs(containerRef.require().id())
                .frames();
            assertThat(frames.allLines())
                .containsExactly("Hello World 1", "Hello World 123");
        }

        @Test
        @DisplayName("logs stdout should return expected lines")
        void logs_stdout_should_return_expected_lines() {
            var stdout = subject
                .containers()
                .logs(containerRef.require().id())
                .stdOut();
            assertThat(stdout)
                .containsExactly("Hello World 1", "Hello World 123");
        }

        @Test
        @DisplayName("logs stderr should return empty")
        void logs_stderr_should_return_empty() {
            var stdErr = subject
                .containers()
                .logs(containerRef.require().id())
                .stdErr();
            assertThat(stdErr).isEmpty();
        }

        @Test
        @DisplayName("logs 'asyncStdOut' should return expected lines")
        void logs_asyncstdout_should_return_expected_lines() {
            var stdout = subject
                .containers()
                .logs(containerRef.require().id())
                .asyncStdOut()
                .join();
            assertThat(stdout.collect(Collectors.toList()))
                .containsExactly("Hello World 1", "Hello World 123");
        }

        @Test
        @DisplayName("logs 'asyncStdErr' should return empty")
        void logs_asyncstderr_should_return_empty() {
            var stdErr = subject
                .containers()
                .logs(containerRef.require().id())
                .asyncStdErr()
                .join();
            assertThat(stdErr.collect(Collectors.toList())).isEmpty();
        }

        @Test
        @DisplayName("logs 'asyncFrames' should return expected lines")
        void logs_asyncframes_should_return_expected_lines() {
            var frames = subject
                .containers()
                .logs(containerRef.require().id())
                .asyncFrames()
                .join();
            assertThat(frames.allLines())
                .containsExactly("Hello World 1", "Hello World 123");
        }

        @Test
        @DisplayName("inspect should return correct root data")
        void inspect_should_return_correct_root_data() {
            var actual = subject.containers().inspect(containerRef.require().id());
            assertThat(actual.id()).isEqualTo(containerRef.require().id());
            assertThat(actual.name()).isNotEmpty();
            assertThat(actual.createdAt()).is(today());
            assertThat(actual.args()).containsExactlyElementsOf(DockerImagesStash.smallImageBusyCommand().args());
            assertThat(actual.path()).isEqualTo(DockerImagesStash.smallImageBusyCommand().command());
            assertThat(actual.processLabel()).isEmpty();
            assertThat(actual.platform()).isEqualTo("linux");
            assertThat(actual.driver()).isNotEmpty();
            assertThat(actual.hostsPath()).isNotEmpty();
            assertThat(actual.hostnamePath()).isNotEmpty();
            assertThat(actual.restartCount()).isZero();
            assertThat(actual.mountLabel()).isEmpty();
            assertThat(actual.resolvConfPath()).isNotEmpty();
            assertThat(actual.logPath()).isNotEmpty();
        }

        @Test
        @DisplayName("inspect should return correct container config")
        void inspect_should_return_correct_container_config() {
            var actual = subject.containers().inspect(containerRef.require().id());
            var containerConfig = actual.config();
            assertThat(containerConfig.labels()).containsAllEntriesOf(containerLabels);
            assertThat(containerConfig.env()).containsAllEntriesOf(containerEnv);
            assertThat(containerConfig.cmd()).containsExactlyElementsOf(DockerImagesStash.smallImageBusyCommand().commandWithArgs());
            assertThat(containerConfig.tty()).isFalse();
            assertThat(containerConfig.attachStdin()).isFalse();
            assertThat(containerConfig.attachStder()).isFalse();
            assertThat(containerConfig.openStdin()).isFalse();
            assertThat(containerConfig.entrypoint()).isEmpty();
            assertThat(containerConfig.workingDir())
                .satisfiesAnyOf(
                    dir -> assertThat(dir).isEmpty(),
                    dir -> assertThat(dir).hasValue("/")
                );
            assertThat(containerConfig.hostname()).isNotEmpty();
        }

        @Test
        @DisplayName("inspect should return correct network data")
        void inspect_should_return_correct_network_data() {
            var actual = subject.containers().inspect(containerRef.require().id());
            var containerNetwork = actual.network();
            assertThat(containerNetwork.gateway()).isNotEmpty();
            assertThat(containerNetwork.ipAddress()).isNotEmpty();
            assertThat(containerNetwork.ipPrefixLen()).isPositive();
            assertThat(containerNetwork.macAddress()).isNotEmpty();
            assertThat(containerNetwork.bridge()).isEmpty();
            assertThat(containerNetwork.globalIpv6PrefixLen()).isZero();
            assertThat(containerNetwork.globalIpv6Address()).isEmpty();
            assertThat(containerNetwork.linkLocalIpv6Address()).isEmpty();
            assertThat(containerNetwork.linkLocalIpv6PrefixLen()).isZero();
            assertThat(containerNetwork.ipv6Gateway()).isEmpty();
            assertThat(containerNetwork.hairpinMode()).isFalse();
            assertThat(containerNetwork.endpointId()).isNotEmpty();
            assertThat(containerNetwork.sandboxId()).isNotEmpty();
            assertThat(containerNetwork.sandboxKey()).isNotEmpty();
            assertThat(containerNetwork.secondaryIpAddresses()).isEmpty();
            assertThat(containerNetwork.secondaryIpV6Addresses()).isEmpty();
        }

        @Test
        @DisplayName("inspect should return correct state data")
        void inspect_should_return_correct_state_data() {
            var actual = subject.containers().inspect(containerRef.require().id());
            var containerState = actual.state();
            assertThat(containerState.status()).isEqualTo(HtContainerStatus.RUNNING);
            assertThat(containerState.pid()).isPositive();
            assertThat(containerState.exitCode()).isZero();
            assertThat(containerState.startedAt()).isNotNull();
            assertThatThrownBy(containerState::finishedAt).hasMessageContaining("not yet finished");
            assertThat(containerState.error()).isEmpty();
            assertThat(containerState.running()).isTrue();
            assertThat(containerState.paused()).isFalse();
            assertThat(containerState.restarting()).isFalse();
            assertThat(containerState.oomKilled()).isFalse();
            assertThat(containerState.dead()).isFalse();
        }

        @Test
        @DisplayName("inspect should return correct graph driver data")
        void inspect_should_return_correct_graph_driver_data() {
            var actual = subject.containers().inspect(containerRef.require().id());
            var containerGraphDriver = actual.graphDriver();
            assertThat(containerGraphDriver.data()).isNotEmpty();
            assertThat(containerGraphDriver.name()).isNotEmpty();
        }

        @Test
        @DisplayName("images list should find default small image")
        void images_list_should_find_default_small_image() {
            var images = subject.images().list().collect();

            var maybeImage = images.stream()
                .flatMap(image -> image.inspect().tags())
                .filter(imageTag -> DockerImagesStash.defaultSmall().equals(imageTag.repository() + ":" + imageTag.tag()))
                .findAny();

            assertThat(maybeImage).isPresent();
        }

        @Test
        @DisplayName("asd")
        void asd() throws Exception {
            var before = subject.containers().logs(containerRef.require().id()).frames().allLines().toList();
            System.out.println(before);

            subject.containers().execInContainer(
                containerRef.require().id(),
                "sh",
                List.of("-c", "echo $((1 + 1)) && echo $((2 + 2))")
            ).exec();

            var after1 = subject.containers().logs(containerRef.require().id()).frames().allLines().toList();
            System.out.println(after1);
            Thread.sleep(2000);

            var after2 = subject.containers().logs(containerRef.require().id()).frames().allLines().toList();
            System.out.println(after2);

        }

        @Test
        @Disabled
        @DisplayName("'execInContainer' should return expected output")
        void execincontainer_should_return_expected_output() {
            subject.containers().execInContainer(
                containerRef.require().id(),
                "sh",
                List.of("-c", "echo 123 > file.txt")
            ).exec();
        }
    }
}
