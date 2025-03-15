package io.huskit.containers.http;

import io.huskit.common.reactive.One;
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
    public One<Stream<HtImageView>> stream() {
        return dockerSpec.socket().send(
            PushIn.of(
                new Request(
                    dockerSpec.requests().get(listImagesSpec)
                ),
                new PushJsonArray()
            )
        ).map(
            response ->
                response.body().value().stream()
                    .map(this::mapToImageView)
        );
    }

    private HtImageView mapToImageView(Map<String, Object> map) {
        return new DefHtImageView(
            (String) map.get("Id"),
            () -> new MapHtImageRichView(map)
        );
    }
}
