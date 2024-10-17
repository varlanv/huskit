package io.huskit.containers.http;

import java.util.Map;

public interface HtUrl {

    String url();

    default Map<String, Object> body() {
        return Map.of();
    }

    static HtUrl of(String url) {
        return () -> url;
    }
}
