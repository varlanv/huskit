package io.huskit.containers.http;

import io.huskit.common.Sneaky;
import io.huskit.common.function.MemoizedSupplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.NonFinal;

import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

final class Npipe implements DockerSocket {

    MemoizedSupplier<Npipe.State> stateSupplier;

    Npipe(String socketFile) {
        this.stateSupplier = MemoizedSupplier.of(() -> new Npipe.State(socketFile));
    }

    @Override
    public <T> Http.Response<T> send(DockerSocket.Request<T> request) {
        return sendAsyncInternal(request).join();
    }

    @Override
    public <T> CompletableFuture<Http.Response<T>> sendAsync(DockerSocket.Request<T> request) {
        return sendAsyncInternal(request);
    }

    private <T> CompletableFuture<Http.Response<T>> sendAsyncInternal(DockerSocket.Request<T> request) {
        var state = stateSupplier.get();
        return state
                .write(request)
                .thenCompose(r -> read(
                                new ReadState<>(
                                        request,
                                        state.decoder
                                )
                        )
                )
                .thenApply(r -> {
                    request.expectedStatus().map(Request.ExpectedStatus::status).ifPresent(expectedStatus -> {
                        if (!Objects.equals(r.head().status(), expectedStatus)) {
                            throw new RuntimeException(
                                    String.format(
                                            "Expected status %d but received %d",
                                            expectedStatus, r.head().status()
                                    )
                            );
                        }
                    });
                    return r;
                })
                .whenComplete((response, throwable) -> {
                    if (state.isDirtyConnection().get()) {
                        state.resetConnection();
                    }
                    state.lock.release();
                });
    }

    private <T> CompletableFuture<Http.Response<T>> read(ReadState<T> readState) {
        return readLoop(readState)
                .thenApply(ReadState::toResponse);
    }

    private <T> CompletableFuture<ReadState<T>> readLoop(ReadState<T> readState) {
        return readAndProcess(readState)
                .thenCompose(r -> {
                    if (r.shouldReadMore) {
                        return readLoop(r);
                    } else {
                        return readState.request
                                .repeatReadPredicate()
                                .map(repeatRead -> {
                                    var predicate = repeatRead.predicate();
                                    var backoff = repeatRead.backoff().toMillis();
                                    for (var line : readState.buildBody().split("\n")) {
                                        if (predicate.test(line.trim())) {
                                            r.isKeepReading = false;
                                            break;
                                        }
                                    }
                                    if (r.isKeepReading) {
                                        try {
                                            if (backoff > 0) {
                                                Thread.sleep(backoff);
                                            }
                                            var rs = new ReadState<>(r.request, r.decoder);
                                            rs.isHead = false;
                                            return readLoop(rs);
                                        } catch (InterruptedException e) {
                                            throw Sneaky.rethrow(e);
                                        }
                                    } else {
                                        return CompletableFuture.completedFuture(r);
                                    }
                                })
                                .orElseGet(() -> CompletableFuture.completedFuture(r));
                    }
                });
    }

    private <T> CompletableFuture<ReadState<T>> readAndProcess(ReadState<T> readState) {
        return readOnce(readState)
                .thenApply(r -> {
                    processBuffer(r);
                    return r;
                });
    }

    @SneakyThrows
    private <T> void processBuffer(ReadState<T> readState) {
        readState.readBuffer.flip();
        readState.chars = readState.decoder.decode(readState.readBuffer);
        if (readState.isHead) {
            readHead(readState);
        }
        if (!readState.isHead) {
            readBody(readState);
        }
        readState.decoder.reset();
        readState.readBuffer.clear();
    }

    private <T> void readHead(ReadState<T> readState) {
        while (readState.chars.hasRemaining()) {
            var ch = readState.chars.get();
            var prevCh = readState.prevChar;
            var isCrLf = ch == '\n' && prevCh == '\r';
            if (isCrLf) {
                if (readState.prevLineWasEmpty) {
                    readState.isHead = false;
                    readState.isChunked = "chunked".equals(readState.headers.get("Transfer-Encoding"));
                    readState.prevLineWasEmpty = false;
                    readState.prevChar = 0;
                    break;
                }
            } else {
                if (readState.prevLineWasEmpty && ch != '\n' && ch != '\r') {
                    readState.prevLineWasEmpty = false;
                }
            }
            if (readState.headLineCount == 0) {
                if (readState.statusReads >= 0 && readState.statusReads <= 3) {
                    if (readState.statusReads == 0) {
                        readState.status = (ch - '0') * 100;
                    } else if (readState.statusReads == 1) {
                        readState.status = readState.status + (ch - '0') * 10;
                    } else if (readState.statusReads == 2) {
                        readState.status = readState.status + (ch - '0');
                    }
                    readState.statusReads++;
                } else {
                    if (ch == ' ' && readState.status == -1) {
                        readState.statusReads++;
                    }
                }
            } else if (readState.isHeaderKeyPart) {
                if (ch == ':') {
                    readState.isHeaderKeyPart = false;
                    readState.lineBuffer.flip();
                    readState.currentHeaderKey = readState.lineBuffer.toString();
                    readState.clearLineBuffer();
                    readState.isHeaderValuePart = true;
                    readState.isHeaderValueSkipPart = true;
                } else {
                    if (!isCrLf) {
                        readState.writeToLineBuffer(ch);
                    }
                }
            } else if (readState.isHeaderValuePart) {
                if (readState.isHeaderValueSkipPart) {
                    readState.isHeaderValueSkipPart = false;
                } else {
                    if (isCrLf) {
                        readState.lineBuffer.flip();
                        readState.lineBuffer.limit(readState.lineBuffer.limit() - 1);
                        var headerVal = readState.lineBuffer.toString();
                        readState.headers.put(readState.currentHeaderKey, headerVal);
                    } else {
                        readState.writeToLineBuffer(ch);
                    }
                }
            }
            if (isCrLf) {
                readState.prevLineWasEmpty = true;
                readState.headLineCount++;
                readState.isHeaderKeyPart = true;
                readState.isHeaderValuePart = false;
                readState.clearLineBuffer();
            }
            readState.prevChar = ch;
        }
    }

    private <T> void readBody(ReadState<T> readState) {
        if (readState.status == 204) {
            readState.shouldReadMore = false;
            readState.bodyNotPresent = true;
        } else {
            if (readState.isChunked) {
                if (readState.chars.remaining() == 0) {
                    readState.shouldReadMore = true;
                } else {
                    if (readState.bodyLineCount == 0) {
                        readState.isChunkSizeLine = true;
                    }
                    while (readState.chars.hasRemaining()) {
                        var ch = readState.chars.get();
                        if (readState.isChunkSizeLine) {
                            if (ch == '\r') {
                                readState.chunkSizeBuffer.flip();
                                readState.currentChunkSize = Integer.parseInt(readState.chunkSizeBuffer.toString(), 16);
                                readState.chunkSizeBuffer.clear();
                                if (readState.currentChunkSize == 0) {
                                    readState.shouldReadMore = false;
                                    break;
                                }
                            } else if (ch != '\n') {
                                readState.chunkSizeBuffer.put(ch);
                            }
                        } else if (readState.currentChunkSize > 0 && ch != '\r' && ch != '\n') {
                            readState.writeToBodyBuffer(ch);
                        }
                        if (ch == '\n' && readState.prevChar == '\r') {
                            readState.isChunkSizeLine = !readState.isChunkSizeLine;
                            readState.bodyLineCount++;
                            readState.prevLineWasEmpty = true;
                        }
                        readState.prevChar = ch;
                    }
                    if (readState.prevLineWasEmpty && readState.resultSize != readState.chars.remaining() && readState.request.repeatReadPredicate().isPresent()) {
                        readState.shouldReadMore = false;
                    }
                }
            } else {
                if (!readState.shouldReadMore) {
                    if (readState.chars.array()[readState.chars.limit() - 1] == '\n') {
                        readState.chars.limit(readState.chars.limit() - 1);
                    }
                }
                readState.writeToBodyBuffer(readState.chars);
            }
        }
    }

    private <T> CompletableFuture<ReadState<T>> readOnce(ReadState<T> readState) {
        var readOpCompletion = new CompletableFuture<ReadState<T>>();
        var state = stateSupplier.get();
        state.channel.read(readState.readBuffer, 0, readState, new CompletionHandler<>() {

            @Override
            public void completed(Integer result, ReadState<T> readState) {
                readState.resultSize = result;
                readOpCompletion.complete(readState);
            }

            @Override
            public void failed(Throwable exc, ReadState readState) {
                readOpCompletion.completeExceptionally(exc);
            }
        });
        return readOpCompletion;
    }


    @Getter
    private static class State {

        @NonFinal
        AsynchronousFileChannel channel;
        String socketFile;
        CharsetDecoder decoder;
        CharsetEncoder encoder;
        Semaphore lock;
        AtomicBoolean isDirtyConnection;

        @SneakyThrows
        private State(String socketFile) {
            this.socketFile = socketFile;
            this.channel = openChannel();
            this.decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
            this.encoder = StandardCharsets.UTF_8.newEncoder();
            this.lock = new Semaphore(1);
            this.isDirtyConnection = new AtomicBoolean(false);
        }

        @SneakyThrows
        private AsynchronousFileChannel openChannel() {
            return AsynchronousFileChannel.open(
                    Paths.get(socketFile),
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE
            );
        }

        public CompletableFuture<Integer> write(DockerSocket.Request<?> request) {
            var body = request.http().body();
            var bb = ByteBuffer.wrap(body);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    var writeCompletion = new CompletableFuture<Integer>();
                    lock.acquire();
                    System.out.println("Sending request body -> " + new String(body, StandardCharsets.UTF_8));
                    channel.write(bb, 0, null, new CompletionHandler<>() {

                        @Override
                        public void completed(Integer result, Object attachment) {
                            if (request.repeatReadPredicate().isPresent()) {
                                isDirtyConnection.set(true);
                            }
                            writeCompletion.complete(result);
                        }

                        @Override
                        public void failed(Throwable exc, Object attachment) {
                            writeCompletion.completeExceptionally(exc);
                        }
                    });
                    return writeCompletion.join();
                } catch (InterruptedException e) {
                    throw Sneaky.rethrow(e);
                }
            });
        }

        @SneakyThrows
        public void resetConnection() {
            channel.close();
            channel = openChannel();
            isDirtyConnection().set(false);
        }
    }

    @SneakyThrows
    public void close() {
        if (stateSupplier.isInitialized()) {
            stateSupplier.get().channel().close();
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class MyHttpResponse<T> implements Http.Response<T> {

        Http.Head head;
        Http.Body<T> body;
    }

    @RequiredArgsConstructor
    public static class HttpFlow {

        @Getter
        Reader reader;
        String body;
        @Getter
        Http.Head head;

        public String string() {
            return body;
        }
    }

    @RequiredArgsConstructor
    private static class ReadState<T> {

        DockerSocket.Request<T> request;
        CharsetDecoder decoder;
        int readBufferSize = 4096;
        int lineBufferSize = 256;
        int chunkSizeBufferSize = 16;
        ByteBuffer readBuffer = ByteBuffer.allocate(readBufferSize);
        @NonFinal
        CharBuffer lineBuffer = CharBuffer.allocate(lineBufferSize);
        @NonFinal
        CharBuffer bodyBuffer;
        CharBuffer chunkSizeBuffer = CharBuffer.allocate(chunkSizeBufferSize);
        @NonFinal
        CharBuffer chars;
        Map<String, String> headers = new HashMap<>();
        @NonFinal
        String currentHeaderKey;
        @NonFinal
        int headLineCount;
        @NonFinal
        int status = -1;
        @NonFinal
        int statusReads = -1;
        @NonFinal
        boolean isHead = true;
        @NonFinal
        char prevChar;
        @NonFinal
        boolean prevLineWasEmpty = false;
        @NonFinal
        boolean isHeaderKeyPart = true;
        @NonFinal
        boolean isHeaderValuePart = false;
        @NonFinal
        boolean isHeaderValueSkipPart = false;
        @NonFinal
        boolean shouldReadMore = false;
        @NonFinal
        boolean isChunked = false;
        @NonFinal
        boolean bodyNotPresent = false;
        @NonFinal
        int currentChunkSize = -1;
        @NonFinal
        boolean isChunkSizeLine = false;
        @NonFinal
        int bodyLineCount;
        @NonFinal
        int resultSize;
        @NonFinal
        boolean isKeepReading = true;

        public void clearLineBuffer() {
            lineBuffer.clear();
        }

        public void writeToLineBuffer(char ch) {
            if (lineBuffer.remaining() == 0) {
                var newBuffer = CharBuffer.allocate(lineBuffer.capacity() * 2);
                lineBuffer.flip();
                newBuffer.put(lineBuffer);
                lineBuffer = newBuffer;
            }
            lineBuffer.put(ch);
        }

        public void writeToBodyBuffer(char ch) {
            if (bodyBuffer == null) {
                bodyBuffer = CharBuffer.allocate(readBufferSize);
            } else if (bodyBuffer.remaining() == 0) {
                var newBuffer = CharBuffer.allocate(bodyBuffer.capacity() * 2);
                bodyBuffer.flip();
                newBuffer.put(bodyBuffer);
                bodyBuffer = newBuffer;
            }
            bodyBuffer.put(ch);
        }

        public void writeToBodyBuffer(CharBuffer chars) {
            if (bodyBuffer == null) {
                bodyBuffer = CharBuffer.allocate(readBufferSize);
            } else if (bodyBuffer.remaining() < chars.remaining()) {
                var newBuffer = CharBuffer.allocate(bodyBuffer.capacity() + chars.remaining());
                bodyBuffer.flip();
                newBuffer.put(bodyBuffer);
                bodyBuffer = newBuffer;
            }
            bodyBuffer.put(chars);
        }

        @SneakyThrows
        public MyHttpResponse<T> toResponse() {
            var body = buildBody();
            return new MyHttpResponse<>(
                    new DfHead(status, headers),
                    new DfBody<>(
                            request.action().apply(
                                    new HttpFlow(
                                            new StringReader(body),
                                            body,
                                            new DfHead(
                                                    status,
                                                    headers
                                            )
                                    )
                            )
                    )
            );
        }

        private String buildBody() {
            if (shouldReadMore || status == -1) {
                throw new IllegalStateException("Cannot create response from incomplete state");
            }
            String body;
            if (bodyNotPresent) {
                body = "";
            } else {
                bodyBuffer.flip();
                body = bodyBuffer.toString().trim();
            }
            return body;
        }
    }
}
