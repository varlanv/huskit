package io.huskit.containers.api.image;

import io.huskit.common.reactive.One;

import java.util.stream.Stream;

public interface HtListImages {

    One<Stream<HtImageView>> stream();
}
