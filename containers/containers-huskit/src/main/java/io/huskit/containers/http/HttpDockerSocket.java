package io.huskit.containers.http;

import io.huskit.common.Log;
import io.huskit.common.function.MemoizedSupplier;
import io.huskit.common.reactive.Many;
import io.huskit.common.reactive.One;
import io.huskit.common.reactive.PushIn;
import io.huskit.common.reactive.PushOut;
import lombok.*;
import lombok.experimental.NonFinal;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

final class HttpDockerSocket implements DockerSocket {

    // TODO - if I change this to lower value - some containers fail to start. Needs fixing.
    private static final Integer BUFFER_SIZE = 8142;
    ScheduledExecutorService executor;
    MemoizedSupplier<HttpChannel> stateSupplier;
    AtomicBoolean isStopped = new AtomicBoolean(false);

    HttpDockerSocket(Supplier<HttpAsyncChannel> asyncChannelSupplier, ScheduledExecutorService executor, Log log) {
        this.executor = executor;
        this.stateSupplier = MemoizedSupplier.of(() -> {
            DockerOnShutdown.register(this::release);
            return new HttpChannel(asyncChannelSupplier, executor, log, BUFFER_SIZE);
        });

    }

    @Override
    public <T> One<Http.Response<T>> send(PushIn<Request, T, ByteBuffer> request) {
        return stateSupplier.get()
            .writeAndReadAsync(
                PushIn.of(
                    request.request(),
                    new PushHttp<>(
                        new PushHead(),
                        request
                    )
                )
            );
    }

    @Override
    public void release() {
        if (isStopped.compareAndSet(false, true)) {
            stateSupplier.ifInitialized(HttpChannel::close);
            executor.shutdownNow();
        }
    }
}

@RequiredArgsConstructor
final class NpipeRead<T> {

    Supplier<One<ByteBuffer>> bytesSupplier;
    ScheduledExecutorService executorService;

    One<T> pushTo(PushOut<T, ByteBuffer> action) {
        return act(action);
    }

    private One<T> act(PushOut<T, ByteBuffer> pushOut) {
        return bytesSupplier.get().flatMap(
            buffer -> {
                try {
                    Optional<T> push = pushOut.push(buffer);
                    return push
                        .map(result -> One.from().item(result))
                        .orElseGet(() -> act(pushOut));
                } catch (Exception e) {
                    return One.from().error(e);
                }
            }
        );
    }
}

final class HttpChannel implements AutoCloseable {

    @NonFinal
    @Getter
    volatile HttpAsyncChannel channel;
    Supplier<HttpAsyncChannel> asyncChannelSupplier;
    ScheduledExecutorService executor;
    DockerChannelIn in;
    DockerChannelOut out;
    SyncCallback syncCallback;

    HttpChannel(Supplier<HttpAsyncChannel> asyncChannelSupplier,
                ScheduledExecutorService executor,
                Log log,
                Integer bufferSize) {
        this.asyncChannelSupplier = asyncChannelSupplier;
        this.executor = executor;
        this.channel = asyncChannelSupplier.get();
        this.syncCallback = new SyncCallback(log);
        this.in = new DockerChannelIn(
            () -> channel,
            syncCallback,
            log
        );
        this.out = new DockerChannelOut(
            () -> channel,
            bufferSize,
            log
        );
    }

    <T> One<T> writeAndReadAsync(PushIn<Request, T, ByteBuffer> pushRequest) {
        return in.write(pushRequest.request())
            .runOnComplete(() ->
                syncCallback.removeFromQueueAndStartNext(
                    pushRequest.request(),
                    () -> {
                        if (pushRequest.request().dirtiesConnection()) {
                            resetConnection();
                        }
                    }
                ))
            .thenFlatMany(out.readToBufferAsync())
            .select().one(bytes -> {
                try {
                    var pushResult = pushRequest.response().push(bytes);
                    return One.from().item(pushResult.isPresent());
                } catch (Exception e) {
                    return One.from().error(e);
                }
            })
            .thenFlat(One.from().optional(() -> pushRequest.response().value()));
    }

    @Locked
    @SneakyThrows
    void resetConnection() {
        channel.close();
        channel = asyncChannelSupplier.get();
    }


    @Override
    public void close() throws Exception {
        channel.close();
    }
}

@RequiredArgsConstructor
final class DockerChannelIn {

    Supplier<HttpAsyncChannel> channel;
    SyncCallback syncCallback;
    Log log;

    One<Void> write(Request request) {
        if (request.http().body().length == 0) {
            log.error(() -> "Cannot write empty body");
            return One.from().error(new IllegalArgumentException("Cannot write empty body"));
        } else {
            return One.from().emitter(emitter -> {
                    syncCallback.placeInQueueAndTryStart(
                        request,
                        () -> {
                            log.debug(
                                () -> "Writing to channel: "
                                    + System.lineSeparator()
                                    + new String(
                                    request.http().body(),
                                    StandardCharsets.UTF_8
                                )
                            );
                            channel.get().write(
                                ByteBuffer.wrap(request.http().body()),
                                new CompletionHandler<>() {

                                    @Override
                                    public void completed(Integer result, Object attachment) {
                                        emitter.complete(result);
                                    }

                                    @Override
                                    public void failed(Throwable exc, Object attachment) {
                                        emitter.fail(exc);
                                    }
                                }
                            );
                        }
                    );
                })
                .mapToNothing();
        }
    }
}

final class DockerChannelOut {

    Supplier<HttpAsyncChannel> channel;
    ByteBuffer byteBuffer;
    Log log;

    public DockerChannelOut(Supplier<HttpAsyncChannel> channel, Integer bufferSize, Log log) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than 0");
        }
        this.channel = channel;
        this.byteBuffer = ByteBuffer.allocate(bufferSize);
        this.log = log;
    }

    Many<ByteBuffer> readToBufferAsync() {
        return Many.from().emitter(emitter -> {
            log.debug(() -> "Started reading from channel");
            channel.get().read(
                byteBuffer.clear(),
                new CompletionHandler<>() {

                    @Override
                    public void completed(Integer result, Object attachment) {
                        byteBuffer.flip();
                        log.debug(
                            () -> "Completed reading from channel: " + System.lineSeparator() + new String(
                                byteBuffer.array(),
                                byteBuffer.position(),
                                byteBuffer.limit(),
                                StandardCharsets.UTF_8
                            )
                        );
                        emitter.emit(byteBuffer);
                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {
                        log.error(() -> "Failed to read from channel");
                        emitter.fail(exc);
                    }
                }
            );
        });
    }
}

final class SyncCallback {

    Log log;
    List<Item> actions;

    SyncCallback(Log log) {
        this.log = log;
        this.actions = new ArrayList<>();
    }

    void placeInQueueAndTryStart(Object request, Runnable action) {
        var actionsSize = sync(
            () -> {
                actions.add(
                    new SyncCallback.Item(
                        request,
                        action
                    )
                );
                var size = actions.size();
                if (size == 1) {
                    log.debug(() -> "Starting action immediately as it is the only one in the queue");
                } else {
                    log.debug(() -> "Placed action in queue, will wait for " + size + " previous actions to finish");
                }
                return size;
            }
        );
        if (actionsSize == 1) {
            action.run();
        }
    }

    void removeFromQueueAndStartNext(Request request, Runnable whenRemovedCallback) {
        sync(
            () -> removeFromQueueAndTakeNext(
                request,
                whenRemovedCallback
            )
        ).run();
    }

    private synchronized <T> T sync(Supplier<T> action) {
        return action.get();
    }

    private Runnable removeFromQueueAndTakeNext(Request request, Runnable whenRemovedCallback) {
        var indexOfAction = -1;
        for (var idx = 0; idx < actions.size(); idx++) {
            var item = actions.get(idx);
            if (item.request() == request) {
                indexOfAction = idx;
                break;
            }
        }
        if (indexOfAction == -1) {
            throw new IllegalStateException("Failed to find action for request");
        }
        if (actions.size() == 1) {
            actions.clear();
            log.debug(() -> "No more actions in queue");
            whenRemovedCallback.run();
            return () -> {
            };
        } else {
            actions.remove(indexOfAction);
            log.debug(() -> "Starting next action in queue, " + actions.size() + " actions remaining");
            whenRemovedCallback.run();
            return () -> actions.get(0).action().run();
        }
    }

    @Value
    static class Item {

        Object request;
        Runnable action;
    }
}
