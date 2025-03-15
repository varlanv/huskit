package io.huskit.containers.http;

import io.huskit.common.reactive.One;
import io.huskit.common.reactive.PushIn;
import io.huskit.containers.api.image.HtPullImages;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class HtHttpPullImages implements HtPullImages {

    HtHttpDockerSpec dockerSpec;
    HtHttpPullImagesSpec pullImagesSpec;

    @Override
    public One<Void> exec() {
        return dockerSpec.socket().sendPushAsync(
                PushIn.of(
                    new Request(
                        dockerSpec.requests().post(pullImagesSpec)
                    ).withExpectedStatus(200),
                    new PushRaw()
                )
            )
            .mapToNothing();
    }
}
