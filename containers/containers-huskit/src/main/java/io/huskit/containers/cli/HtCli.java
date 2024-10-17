package io.huskit.containers.cli;

import io.huskit.common.Nothing;
import io.huskit.common.function.MemoizedSupplier;
import io.huskit.containers.model.CommandType;
import lombok.Locked;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor
public class HtCli {

    @With
    HtCliDckrSpec dockerSpec;
    Shells shells;
    MemoizedSupplier<DockerShell> process = MemoizedSupplier.of(this::createProcess);
    AtomicBoolean isClosed = new AtomicBoolean();

    public void sendCommand(HtCommand command) {
        sendCommand(command, Function.identity());
    }

    @Locked
    public <T> T sendCommand(HtCommand command, Function<CommandResult, T> resultConsumer) {
        if (isClosed.get()) {
            throw new IllegalStateException("Cli is closed and  cannot be used anymore");
        }
        var dockerShellProcess = process.get();
        return dockerShellProcess.sendCommand(command, resultConsumer);
    }

    public void sendCommand(HtCommand command, Consumer<CommandResult> resultConsumer) {
        sendCommand(command, result -> {
            resultConsumer.accept(result);
            return Nothing.instance();
        });
    }

    private DockerShell createProcess() {
        return new DockerShell(this, dockerSpec, shells);
    }

    public void close() {
        if (process.isInitialized()) {
            process.get().stop();
        }
        isClosed.set(true);
    }

    static class DockerShell {

        private static final String RUN_LINE_MARKER = "__HUSKIT_RUN_MARKER__";
        private static final String CLEAR_LINE_MARKER = "__HUSKIT_CLEAR_MARKER__";
        AtomicBoolean isStopped = new AtomicBoolean();
        HtCli parent;
        CliRecorder recorder;
        Queue<String> containerIdsForCleanup;
        Boolean cleanupOnClose;
        Shell shell;
        HtCliDckrSpec dockerSpec;

        public DockerShell(HtCli parent, HtCliDckrSpec dockerSpec, Shells shells) {
            this.parent = parent;
            this.recorder = dockerSpec.recorder();
            this.cleanupOnClose = dockerSpec.isCleanOnClose();
            this.containerIdsForCleanup = new ConcurrentLinkedQueue<>();
            this.shell = shells.take(new ShellPickArg(
                            dockerSpec.shell(),
                            dockerSpec.forwardStdout(),
                            dockerSpec.forwardStderr()
                    )
            );
            this.dockerSpec = dockerSpec;
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        }

        public <T> T sendCommand(HtCommand command, Function<CommandResult, T> resultFunction) {
            if (command.type() == CommandType.CONTAINERS_LOGS_FOLLOW) {
                return new LogFollow(
                        dockerSpec,
                        recorder,
                        containerIdsForCleanup
                ).send(
                        command,
                        resultFunction
                );
            }
            doSendCommand(command);
            shell.echo(RUN_LINE_MARKER);
            return read(command, resultFunction);
        }

        private void doSendCommand(HtCommand command) {
            recorder.record(command);
            shell.clearBuffer(CLEAR_LINE_MARKER);
            shell.write(command.value());
        }

        @SneakyThrows
        private <T> T read(HtCommand command, Function<CommandResult, T> resultConsumer) {
            var commandString = String.join(" ", command.value());
            var lines = new ArrayList<String>();
            var line = shell.outLine().trim();
            var terminatePredicate = command.terminatePredicate();
            var linePredicate = command.linePredicate();
            while (!line.endsWith(RUN_LINE_MARKER)) {
                if (!line.isEmpty()) {
                    if (terminatePredicate.test(line)) {
                        shell.close();
                        lines.add(line);
                        break;
                    }
                    if (!line.endsWith(commandString) && linePredicate.test(line)) {
                        lines.add(line);
                    }
                }
                line = shell.outLine();
            }

            if (command.type() == CommandType.CONTAINERS_RUN || command.type() == CommandType.CONTAINERS_RUN_FOLLOW) {
                containerIdsForCleanup.add(lines.get(0));
            }
            return resultConsumer.apply(new CommandResult(lines));
        }

        private void stop() {
            if (isStopped.compareAndSet(false, true)) {
                if (cleanupOnClose && !containerIdsForCleanup.isEmpty()) {
                    new HtCliRm(parent, new HtCliRmSpec(new LinkedHashSet<>(containerIdsForCleanup)).withForce().withVolumes()).exec();
                }
                shell.close();
            }
        }
    }
}
