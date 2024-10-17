package io.huskit.containers.http;

import org.intellij.lang.annotations.PrintFormat;

import java.nio.charset.StandardCharsets;

class HttpLogsSpec {

    @PrintFormat
    private static final String requestFormat = "%s %s HTTP/1.1%n"
            + "Host: %s%n"
            + "Connection: keep-alive%n"
            + "%n";
    String containerId;

    HttpLogsSpec(CharSequence containerId) {
        this.containerId = containerId.toString();
    }

    public Http.Request toRequest() {
        return new DfHttpRequest(
                String.format(
                        requestFormat,
                        "GET", "/containers/" + containerId + "/logs?stdout=true&stderr=true", "localhost"
                ).getBytes(StandardCharsets.UTF_8)
        );
    }
}
