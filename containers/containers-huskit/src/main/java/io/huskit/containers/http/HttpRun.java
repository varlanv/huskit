package io.huskit.containers.http;

import io.huskit.common.reactive.One;
import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.run.HtRun;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
final class HttpRun implements HtRun {

    HttpCreate httpCreate;
    HttpRunSpec httpRunSpec;
    Function<String, HttpStart> httpStartFromContainerId;
    Function<String, HttpLogs> httpLogsFromContainerId;
    HtHttpDockerSpec dockerSpec;

    @Override
    public One<HtContainer> exec() {
//        var time = System.currentTimeMillis();
        return httpCreate.exec()
            .flatMap(
                container -> {
                    var start = httpStartFromContainerId.apply(container.id()).execAsync();
                    return httpRunSpec.lookFor()
                        .map(
                            lookFor -> {
                                var httpLogs = httpLogsFromContainerId.apply(container.id());
                                return start.flatMap(
                                    c -> httpLogs.follow()
                                        .lookFor(lookFor)
                                        .map(ignored -> container)
                                );
                            }
                        )
                        .orElse(start);
                }
            );
//            .runOnComplete(
//                (container, throwable) -> {
//                    if (throwable != null) {
//                        dockerSpec.log().error(
//                            () -> "Failed to start container (time spent - "
//                                + Duration.ofMillis(System.currentTimeMillis() - time)
//                                + ") - " + throwable.getMessage()
//                        );
//                    } else {
//                        dockerSpec.log().debug(
//                            () -> "Container successfully started (time spent - "
//                                + Duration.ofMillis(System.currentTimeMillis() - time)
//                                + "), container id - "
//                                + container.id()
//                        );
//                    }
//                }
//            );
    }
}
