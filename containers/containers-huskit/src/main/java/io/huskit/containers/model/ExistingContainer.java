package io.huskit.containers.model;

import java.time.Duration;
import java.util.Map;

public interface ExistingContainer {

    String id();

    String containerKey();

    Long createdAt();

    Map<String, String> labels();

    boolean isExpired(Duration cleanupAfter);
}
