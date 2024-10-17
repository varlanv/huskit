package io.huskit.containers.http;

import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.HtCreate;
import io.huskit.containers.api.container.HtLazyContainer;
import io.huskit.containers.internal.HtJson;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class HttpCreate implements HtCreate {

    HtHttpDockerSpec dockerSpec;
    HttpCreateSpec httpCreateSpec;
    HttpInspect httpInspect;

    @Override
    public HtContainer exec() {
        return execAsync().join();
    }

    @Override
    public CompletableFuture<HtContainer> execAsync() {
        return dockerSpec.socket()
                .sendAsync(
                        new DockerSocket.Request<>(
                                dockerSpec.requests().post(HtUrl.of(httpCreateSpec.url()), httpCreateSpec.body()),
                                r -> List.of(
                                        HtJson.toMap(
                                                r.reader()
                                        )
                                )
                        ).withExpectedStatus(201)
                )
                .thenApply(response -> {
                    var id = (String) response.body().single().get("Id");
                    return new HtLazyContainer(id, () -> httpInspect.inspect(id));
                });
    }
}
