package io.huskit.containers.http;

import io.huskit.common.Mutable;
import io.huskit.common.Sneaky;
import io.huskit.common.function.MemoizedSupplier;
import io.huskit.common.port.DynamicContainerPort;
import io.huskit.common.port.MappedPort;
import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.HtContainerStatus;
import io.huskit.containers.api.container.logs.LookFor;
import io.huskit.gradle.commontest.DockerImagesStash;
import io.huskit.gradle.commontest.DockerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(HtHttpDckrIntegrationTest.HtHttpDckrTestExtension.class)
class HtHttpDckrIntegrationTest implements DockerIntegrationTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @Execution(ExecutionMode.CONCURRENT)
    void listContainers__withRandomFilters__shouldReturnEmptyList(HtHttpDckr subject) {
        var htContainers = subject.containers().list(spec -> spec
                        .withIdFilter("asd")
                        .withLabelFilter("key", "val")
                        .withLabelFilter("key2")
                        .withNameFilter("someName")
                        .withAll()
                )
                .asList();

        assertThat(htContainers).isEmpty();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @Execution(ExecutionMode.CONCURRENT)
    void docker_alpine_container_spec(HtHttpDckr subject) {
        var containerRef = Mutable.<HtContainer>of();
        var ex = Mutable.<Throwable>of();
        try {
            var containerEnv = Map.of(
                    "ENV1", "ENVVALUE1",
                    "ENV2", "ENVVALUE2"
            );
            var containerLabels = Map.of(
                    "LABEL1", "LABELVALUE1",
                    "LABEL2", "LABELVALUE2"
            );
            var mappedPort1 = new MappedPort(new DynamicContainerPort().hostValue(), 80);
            var mappedPort2 = new MappedPort(new DynamicContainerPort().hostValue(), 8080);
            {
                var container = subject.containers().run(
                                DockerImagesStash.defaultSmall(),
                                spec -> spec
                                        .withCommand(
                                                DockerImagesStash.smallImageBusyCommand().command(),
                                                DockerImagesStash.smallImageBusyCommand().args()
                                        )
                                        .withEnv(containerEnv)
                                        .withLabels(containerLabels)
                                        .withPortBindings(Map.of(
                                                mappedPort1.host(), mappedPort1.container(),
                                                mappedPort2.host(), mappedPort2.container()
                                        ))
                                        .withLookFor("Hello World 2", Duration.ofSeconds(10)))
                        .exec();
                assertThat(container.id()).isNotEmpty();
                containerRef.set(container);
                var containerConfig = container.config();
                assertThat(containerConfig.labels()).containsAllEntriesOf(containerLabels);
                assertThat(containerConfig.env()).containsAllEntriesOf(containerEnv);
                assertThat(container.name()).isNotEmpty();
                var containerNetwork = container.network();
//                assertThat(containerNetwork.ports().get(0)).isIn(mappedPort1, mappedPort2).isNotEqualTo(containerNetwork.ports().get(1));
//                assertThat(containerNetwork.ports().get(1)).isIn(mappedPort1, mappedPort2);
            }
            {
                var logs = subject.containers().logs(containerRef.require().id())
                        .follow()
                        .lookFor(LookFor.word("Hello World 2"))
                        .stream()
                        .collect(Collectors.toList());
                assertThat(logs).containsExactly("Hello World 1", "Hello World 2");
            }

            {
                var logs = subject.containers().logs(containerRef.require().id())
                        .stream()
                        .collect(Collectors.toList());
                assertThat(logs).containsExactly("Hello World 1", "Hello World 2");
            }
            {
                var inspected = subject.containers().inspect(containerRef.require().id());
                {
                    assertThat(inspected.id()).isEqualTo(containerRef.require().id());
                    assertThat(inspected.name()).isNotEmpty();
                    assertThat(inspected.createdAt()).is(today());
                    assertThat(inspected.args()).containsExactlyElementsOf(DockerImagesStash.smallImageBusyCommand().args());
                    assertThat(inspected.path()).isEqualTo(DockerImagesStash.smallImageBusyCommand().command());
                    assertThat(inspected.processLabel()).isEmpty();
                    assertThat(inspected.platform()).isEqualTo("linux");
                    assertThat(inspected.driver()).isNotEmpty();
                    assertThat(inspected.hostsPath()).isNotEmpty();
                    assertThat(inspected.hostnamePath()).isNotEmpty();
                    assertThat(inspected.restartCount()).isZero();
                    assertThat(inspected.mountLabel()).isEmpty();
                    assertThat(inspected.resolvConfPath()).isNotEmpty();
                    assertThat(inspected.logPath()).isNotEmpty();
                }
                {
                    var containerConfig = inspected.config();
                    assertThat(containerConfig.labels()).containsAllEntriesOf(containerLabels);
                    assertThat(containerConfig.env()).containsAllEntriesOf(containerEnv);
                    assertThat(containerConfig.cmd()).containsExactlyElementsOf(DockerImagesStash.smallImageBusyCommand().commandWithArgs());
                    assertThat(containerConfig.tty()).isFalse();
                    assertThat(containerConfig.attachStdin()).isFalse();
                    assertThat(containerConfig.attachStder()).isFalse();
                    assertThat(containerConfig.openStdin()).isFalse();
                    assertThat(containerConfig.entrypoint()).isEmpty();
                    assertThat(containerConfig.workingDir()).isEmpty();
                    assertThat(containerConfig.hostname()).isNotEmpty();
                }
                {
                    var containerNetwork = inspected.network();
//                    assertThat(containerNetwork.ports()).containsExactly(mappedPort1, mappedPort2);
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
//                    assertThatThrownBy(containerNetwork::firstMappedPort).hasMessageContaining("multiple are present");
                }
                {
                    var containerState = inspected.state();
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
                {
                    var containerGraphDriver = inspected.graphDriver();
                    assertThat(containerGraphDriver.data()).isNotEmpty();
                    assertThat(containerGraphDriver.name()).isNotEmpty();
                }
                {
                    var commandResult = subject.containers().execInContainer(
                            containerRef.require().id(),
                            "sh",
                            List.of("-c", "echo $((1 + 1)) && echo $((2 + 2))")
                    ).exec();
                    assertThat(commandResult.lines()).containsExactly("2", "4");
                    assertThat(subject.containers().logs(containerRef.require().id()).stream().collect(Collectors.toList()))
                            .containsExactly("Hello World 1", "Hello World 2");
                }
            }
        } catch (Throwable t) {
            ex.set(t);
        } finally {
            try {
                containerRef.ifPresent(container -> subject.containers().remove(container.id(), spec -> spec.withForce().withVolumes()).exec());
            } catch (Throwable t) {
                if (ex.isPresent()) {
                    var originalEx = ex.require();
                    originalEx.addSuppressed(t);
                    Sneaky.rethrow(originalEx);
                } else {
                    Sneaky.rethrow(t);
                }
            }
        }
    }

    static final class HtHttpDckrTestExtension implements ParameterResolver, AfterAllCallback {

        private static final MemoizedSupplier<HtHttpDckr> subjectSupplier = MemoizedSupplier.of(() -> new HtHttpDckr().withCleanOnClose(true));
        private static final ConcurrentMap<String, Supplier<Object>> parameters = new ConcurrentHashMap<>(
                Map.of(
                        HtHttpDckr.class.getName(), subjectSupplier::get
                )
        );

        @Override
        public void afterAll(ExtensionContext context) {
            if (subjectSupplier.isInitialized()) {
                subjectSupplier.get().close();
            }
        }

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
            return parameters.containsKey(parameterContext.getParameter().getType().getName());
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
            return Optional.ofNullable(parameters.get(parameterContext.getParameter().getType().getName()).get())
                    .orElseThrow(() -> new ParameterResolutionException("No supplier found for " + parameterContext.getParameter().getType().getName()));
        }
    }
}
