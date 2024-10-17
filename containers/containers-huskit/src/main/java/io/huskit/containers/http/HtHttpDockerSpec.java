package io.huskit.containers.http;

public interface HtHttpDockerSpec {

    DockerSocket socket();

    Boolean isCleanOnClose();

    HttpRequests requests();
}
