package io.huskit.containers.http;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
class DfHttpRequest implements Http.Request {

    byte[] body;

    DfHttpRequest(byte[]... parts) {
        if (parts.length == 1) {
            this.body = parts[0];
        } else {
            var size = 0;
            for (var part : parts) {
                size += part.length;
            }
            this.body = new byte[size];
            var offset = 0;
            for (var part : parts) {
                System.arraycopy(part, 0, body, offset, part.length);
                offset += part.length;
            }
        }
    }
}
