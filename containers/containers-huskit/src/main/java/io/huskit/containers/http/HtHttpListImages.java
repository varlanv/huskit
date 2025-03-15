package io.huskit.containers.http;

import io.huskit.common.concurrent.FinishFuture;
import io.huskit.common.reactive.PushIn;
import io.huskit.containers.api.image.DefHtImageView;
import io.huskit.containers.api.image.HtImageView;
import io.huskit.containers.api.image.HtListImages;
import io.huskit.containers.api.image.MapHtImageRichView;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Stream;

@RequiredArgsConstructor
final class HtHttpListImages implements HtListImages {

    HtHttpDockerSpec dockerSpec;
    HttpListImagesSpec listImagesSpec;

    @Override
    public Stream<HtImageView> stream() {
        return FinishFuture.finish(
            dockerSpec.socket().sendPushAsync(
                PushIn.of(
                    new Request(
                        dockerSpec.requests().get(listImagesSpec)
                    ),
                    new PushJsonArray()
                )
            ).thenApply(
                response ->
                    response.body().value().stream()
                        .map(this::mapToImageView)
            ),
            dockerSpec.defaultTimeout()
        );
    }

    private HtImageView mapToImageView(Map<String, Object> map) {
        return new DefHtImageView(
            (String) map.get("Id"),
            () -> new MapHtImageRichView(map)
        );
    }
}
