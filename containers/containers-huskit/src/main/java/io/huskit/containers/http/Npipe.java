package io.huskit.containers.http;

import io.huskit.common.function.MemoizedSupplier;
import io.huskit.common.function.ThrowingFunction;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class Npipe implements DockerSocket {

    MemoizedSupplier<Npipe.State> stateSupplier;

    Npipe(String socketFile) {
        this.stateSupplier = MemoizedSupplier.of(() -> new Npipe.State(socketFile));
    }

    @Override
    public <T> Http.Response<T> send(Http.Request request, ThrowingFunction<HttpFlow, List<T>> action) {
        return sendAsyncInternal(request, action).join();
    }

    @Override
    public <T> CompletableFuture<Http.Response<T>> sendAsync(Http.Request request, ThrowingFunction<HttpFlow, List<T>> action) {
        return sendAsyncInternal(request, action);
    }

    private <T> CompletableFuture<Http.Response<T>> sendAsyncInternal(Http.Request request, ThrowingFunction<HttpFlow, List<T>> action) {
        var state = stateSupplier.get();
        return state
                .write(request)
                .thenCompose(r -> read(
                                new ReadState<>(
                                        action,
                                        state.decoder
                                )
                        )
                );
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
                        return CompletableFuture.completedFuture(r);
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
        ByteBuffer cloned = ByteBuffer.allocate(readState.readBuffer.remaining());
        cloned.put(readState.readBuffer.duplicate());
        cloned.flip();

        readState.chars = readState.decoder.decode(readState.readBuffer);
        if (readState.isHead) {
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
                        readState.lineBuffer.clear();
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
                    readState.lineBuffer.clear();
                }
                readState.prevChar = ch;
            }
        }
        if (!readState.isHead) {
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
                                } else {
                                    if (ch != '\n') {
                                        readState.chunkSizeBuffer.put(ch);
                                    }
                                }
                            }
                            var isCrLf = ch == '\n' && readState.prevChar == '\r';
                            if (isCrLf) {
                                readState.isChunkSizeLine = !readState.isChunkSizeLine;
                                readState.bodyLineCount++;
                                readState.prevLineWasEmpty = true;
                            }
                            if (!readState.isChunkSizeLine && readState.currentChunkSize > 0 && !isCrLf && !(ch == '\r' && readState.prevChar == '\n')) {
                                readState.writeToBodyBuffer(ch);
                            }
                            readState.prevChar = ch;
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
        readState.readBuffer.clear();
    }

    private <T> CompletableFuture<ReadState<T>> readOnce(ReadState<T> readState) {
        var readOpCompletion = new CompletableFuture<ReadState<T>>();
        var state = stateSupplier.get();
        state.channel.read(readState.readBuffer, 0, readState, new CompletionHandler<>() {

            @Override
            public void completed(Integer result, ReadState<T> readState) {
                readState.currentResult = result;
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

        AsynchronousFileChannel channel;
        CharsetDecoder decoder;
        CharsetEncoder encoder;

        @SneakyThrows
        private State(String socketFile) {
            this.channel = AsynchronousFileChannel.open(
                    Paths.get(socketFile),
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE
            );
            this.decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE);
            this.encoder = StandardCharsets.UTF_8.newEncoder();
        }

        public CompletableFuture<Integer> write(Http.Request request) {
            var writeCompletion = new CompletableFuture<Integer>();
            var body = request.body();
            channel.write(ByteBuffer.wrap(body), 0, null, new CompletionHandler<>() {

                @Override
                public void completed(Integer result, Object attachment) {
                    writeCompletion.complete(result);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    writeCompletion.completeExceptionally(exc);
                }
            });
            return writeCompletion;
        }
    }

    @SneakyThrows
    public void close() {
        if (stateSupplier.isInitialized()) {
            stateSupplier.get().channel.close();
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class MyHttpResponse<T> implements Http.Response<T> {

        Http.Head head;
        Http.Body<T> body;
    }

    @Getter
    @RequiredArgsConstructor
    public static class HttpFlow {

        Reader reader;
        Http.Head head;
    }

    @RequiredArgsConstructor
    private static class ReadState<T> {

        ThrowingFunction<HttpFlow, List<T>> action;
        CharsetDecoder decoder;
        int readBufferSize = 4096;
        int lineBufferSize = 256;
        @NonFinal
        Integer currentResult;
        @NonFinal
        CharBuffer chars;
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
        Map<String, String> headers = new HashMap<>();
        ByteBuffer readBuffer = ByteBuffer.allocate(readBufferSize);
        @NonFinal
        CharBuffer lineBuffer = CharBuffer.allocate(lineBufferSize);
        @NonFinal
        CharBuffer bodyBuffer;
        @NonFinal
        boolean shouldReadMore = false;
        @NonFinal
        boolean isChunked = false;
        @NonFinal
        boolean bodyNotPresent = false;
        @NonFinal
        String currentHeaderKey;
        @NonFinal
        int currentChunkSize = -1;
        @NonFinal
        boolean isChunkSizeLine = false;
        @NonFinal
        int bodyLineCount;
        CharBuffer chunkSizeBuffer = CharBuffer.allocate(16);

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
            }
            if (bodyBuffer.remaining() == 0) {
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
            }
            if (bodyBuffer.remaining() < chars.remaining()) {
                var newBuffer = CharBuffer.allocate(bodyBuffer.capacity() + chars.remaining());
                bodyBuffer.flip();
                newBuffer.put(bodyBuffer);
                bodyBuffer = newBuffer;
            }
            bodyBuffer.put(chars);
        }

        @SneakyThrows
        public MyHttpResponse<T> toResponse() {
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
            return new MyHttpResponse<>(
                    new DfHead(status, headers),
                    new DfBody<>(
                            action.apply(
                                    new HttpFlow(
                                            new StringReader(body),
                                            new DfHead(
                                                    status,
                                                    headers
                                            )
                                    )
                            )
                    )
            );
        }
    }
}
