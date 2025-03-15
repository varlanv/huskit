package io.huskit.containers.http;

import io.huskit.common.collection.FlexBytes;
import io.huskit.common.number.Hexadecimal;
import io.huskit.common.reactive.PushOut;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;

import java.nio.ByteBuffer;
import java.util.Optional;

@RequiredArgsConstructor
final class PushChunked<T> implements PushOut<T, ByteBuffer> {

    PushOut<T, ByteBuffer> delegate;
    FlexBytes bytes = new FlexBytes();
    @NonFinal
    int skipNext = 0;
    @NonFinal
    boolean isChunkSizePart = true;
    @NonFinal
    int prev;
    Hexadecimal currentChunkSizeHex = Hexadecimal.fromHexChars();

    @Override
    public Optional<T> value() {
        return delegate.value();
    }

    @Override
    public Optional<T> push(ByteBuffer byteBuffer) {
        while (byteBuffer.hasRemaining()) {
            byte b = byteBuffer.get();
            int i = b & 0xFF;
            if (skipNext > 0) {
                skipNext--;
                prev = i;
                continue;
            }
            if (isChunkSizePart) {
                if (i == '\r' || (i == '\n' && prev != '\r')) {
                    isChunkSizePart = false;
                    if (currentChunkSizeHex.intValue() == 0) {
                        return delegate.push(bytes.buffer().flip());
                    }
                    if (i == '\r') {
                        skipNext = 1;
                    }
                } else if (i == '\n') {
                    prev = i;
                    continue;
                } else {
                    currentChunkSizeHex.withHexChar((char) i);
                }
            } else {
                var chunkSize = currentChunkSizeHex.decrement().intValue();
                bytes.append(b);
                if (chunkSize == 0) {
                    isChunkSizePart = true;
                    skipNext = 2;
                }
            }
            prev = i;
        }
        return Optional.empty();
    }
}