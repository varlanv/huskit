package io.huskit.containers.http;

import io.huskit.common.reactive.One;
import io.huskit.common.reactive.PushIn;
import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.HtCreate;
import io.huskit.containers.api.container.HtLazyContainer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class HttpCreate implements HtCreate {

    String imgName;
    HtHttpDockerSpec dockerSpec;
    HttpCreateSpec httpCreateSpec;
    HttpInspect httpInspect;
    LocalImagesStash localImagesStash;

    @Override
    public One<HtContainer> exec() {
        localImagesStash.pullIfAbsent(imgName);
        return dockerSpec.socket()
            .send(
                PushIn.of(
                    new Request(
                        dockerSpec.requests().post(httpCreateSpec)
                    ).withExpectedStatus(201),
                    new PushJsonObject()
                )
            )
            .map(
                response -> {
                    var id = (String) response.body().value().get("Id");
                    return new HtLazyContainer(
                        id,
                        () -> httpInspect.inspect(id).block()
                    );
                }
            );
    }
}
