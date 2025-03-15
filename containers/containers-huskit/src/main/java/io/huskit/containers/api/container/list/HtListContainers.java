package io.huskit.containers.api.container.list;

import io.huskit.common.reactive.One;
import io.huskit.containers.api.container.HtContainer;

import java.util.List;
import java.util.stream.Stream;

public interface HtListContainers {

    One<List<HtContainer>> asList();

    One<Stream<HtContainer>> asStream();
}
