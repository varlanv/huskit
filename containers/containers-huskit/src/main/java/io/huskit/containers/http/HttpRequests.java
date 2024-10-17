package io.huskit.containers.http;

import io.huskit.containers.internal.HtJson;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpRequests {

    byte[] afterUrlPartGet;
    byte[] afterUrlPartPost;

    public HttpRequests() {
        this.afterUrlPartGet = (" "
                + "HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Connection: keep-alive\r\n"
                + "Content-Type: application/json\r\n"
                + "\r\n"
        ).getBytes(StandardCharsets.UTF_8);
        this.afterUrlPartPost = (" "
                + "HTTP/1.1\r\n"
                + "Host: localhost\r\n"
                + "Connection: keep-alive\r\n"
                + "Content-Type: application/json\r\n"
                + "Content-Length: "
//                + "Content-Length: %d\r\n"
//                + "\r\n"
        ).getBytes(StandardCharsets.UTF_8);
    }

    public Http.Request get(HtUrl url) {
        return generate(HttpMethod.GET, url);
    }

    public Http.Request post(HtUrl url, byte[] body) {
        return generate(HttpMethod.POST, url, new String(body, StandardCharsets.UTF_8), body.length);
    }

    public Http.Request post(HtUrl url, Map<String, Object> body) {
        String json = HtJson.toJson(body);
        return generate(HttpMethod.POST, url, json, json.getBytes(StandardCharsets.UTF_8).length);
    }

    private Http.Request generate(HttpMethod method, HtUrl url) {
        var urlBytes = url.url().getBytes(StandardCharsets.UTF_8);
        return new DfHttpRequest(
                method.bytes(),
                urlBytes,
                afterUrlPartGet
        );
    }

    private Http.Request generate(HttpMethod method, HtUrl url, String body, Integer length) {
        var urlBytes = url.url().getBytes(StandardCharsets.UTF_8);
        String sb = length + "\r\n\r\n" + body;
        return new DfHttpRequest(
                method.bytes(),
                urlBytes,
                afterUrlPartPost,
                sb.getBytes(StandardCharsets.UTF_8)
        );
    }
}
