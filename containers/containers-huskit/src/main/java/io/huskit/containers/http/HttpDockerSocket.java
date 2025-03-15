package io.huskit.containers.http;

import io.huskit.common.Log;
import io.huskit.common.function.MemoizedSupplier;
import io.huskit.common.reactive.PushIn;
import io.huskit.common.reactive.PushOut;
import lombok.*;
import lombok.experimental.NonFinal;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

final class HttpDockerSocket implements DockerSocket {

    ScheduledExecutorService executor;
    MemoizedSupplier<HttpChannel> stateSupplier;
    AtomicBoolean isStopped = new AtomicBoolean(false);

    HttpDockerSocket(Supplier<HttpAsyncChannel> asyncChannelSupplier, ScheduledExecutorService executor, Log log) {
        this.executor = executor;
        this.stateSupplier = MemoizedSupplier.of(() -> {
            DockerOnShutdown.register(this::release);
            return new HttpChannel(asyncChannelSupplier, executor, log, 16384);
        });

    }

    @Override
    public <T> CompletableFuture<Http.Response<T>> sendPushAsync(PushIn<Request, T> request) {
        PushIn<Request, Http.Response<T>> responsePushIn = PushIn.of(
            request.request(),
            new HttpPushOut<>(
                new PushHead(),
                request
            )
        );
        return stateSupplier.get().writeAndReadAsync(responsePushIn);
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

    CompletableFuture<T> completion;
    Supplier<CompletableFuture<ByteBuffer>> bytesSupplier;
    ScheduledExecutorService executorService;

    void pushTo(PushOut<T> action) {
        act(action, completion);
    }

    private void act(PushOut<T> action,
                     CompletableFuture<T> completion) {
        bytesSupplier.get().thenAccept(
            buffer -> {
                try {
                    action.push(buffer).ifPresentOrElse(
                        completion::complete,
                        () -> executorService.submit(
                            () -> act(
                                action,
                                completion
                            )
                        )
                    );
                } catch (Exception e) {
                    completion.completeExceptionally(e);
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

    <T> CompletableFuture<T> writeAndReadAsync(PushIn<Request, T> pushRequest) {
        var completion = new CompletableFuture<T>();
        in.write(pushRequest.request())
            .thenRun(
                () -> new NpipeRead<T>(
                    completion,
                    out::readToBufferAsync,
                    executor
                ).pushTo(pushRequest.response())
            );
        return completion.whenComplete(
            (ignore, throwable) -> {
                syncCallback.removeFromQueueAndStartNext(
                    pushRequest.request(),
                    () -> {
                        if (pushRequest.request().dirtiesConnection()) {
                            resetConnection();
                        }
                    }
                );
            }
        );
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

    CompletableFuture<Integer> write(Request request) {
        var completion = new CompletableFuture<Integer>();
        if (request.http().body().length == 0) {
            log.error(() -> "Cannot write empty body");
            completion.completeExceptionally(
                new IllegalArgumentException("Cannot write empty body")
            );
        } else {
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
                                completion.complete(result);
                            }

                            @Override
                            public void failed(Throwable exc, Object attachment) {
                                completion.completeExceptionally(exc);
                            }
                        }
                    );
                }
            );
        }
        return completion;
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

    CompletableFuture<ByteBuffer> readToBufferAsync() {
        var completion = new CompletableFuture<ByteBuffer>();
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
                    completion.complete(byteBuffer);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    log.error(() -> "Failed to read from channel");
                    completion.completeExceptionally(exc);
                }
            }
        );
        return completion;
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
