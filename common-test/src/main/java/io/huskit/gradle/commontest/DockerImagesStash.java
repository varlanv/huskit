package io.huskit.gradle.commontest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DockerImagesStash {

    private static final String REPOSITORY = "public.ecr.aws/docker/library/";
    private static final Queue<String> SMALL_IMAGES = new ConcurrentLinkedQueue<>(
            Stream.of(
                            "3.17.0",
                            "3.17.1",
                            "3.17.2",
                            "3.17.3",
                            "3.17.4",
                            "3.17.5",
                            "3.17.7",
                            "3.17.8",
                            "3.17.9",
                            "3.17.10",
                            "3.18.0",
                            "3.19.0",
                            "3.19.1",
                            "3.19.2",
                            "3.19.3",
                            "3.19.4",
                            "3.20.0",
                            "3.20.1",
                            "3.20.2",
                            "3.20.3"
                    )
                    .map(tag -> REPOSITORY + "alpine:" + tag)
                    .collect(Collectors.toSet()));
    private static final Map<String, String> USED_SMALL_IMAGES = new ConcurrentHashMap<>();
    private static final int SMALL_IMAGES_SIZE = SMALL_IMAGES.size();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (SMALL_IMAGES.size() != SMALL_IMAGES_SIZE) {
                var dashes = "-".repeat(60);
                var message = String.format("%n%s%n%nERROR: [%s] small images were not returned to stash, "
                                + "perhaps there were test failure. Check %s.%n%n%s%n%n",
                        dashes, SMALL_IMAGES_SIZE - SMALL_IMAGES.size(), DockerImagesStash.class, dashes);
                System.err.println(message);
            }
        }));
    }

    @Getter
    @RequiredArgsConstructor
    public static class Command {

        String command;
        List<String> args;

        public List<String> commandWithArgs() {
            return Stream.concat(Stream.of(command), args.stream()).collect(Collectors.toList());
        }
    }

    public static Command smallImageBusyCommand() {
        return new Command("sh", List.of("-c", "echo 'Hello World 1' && echo 'Hello World 2' && tail -f /dev/null"));
    }

    public static Command smallImageInfinitePrintCommand() {
        return new Command("sh", List.of("-c", "count=1; while true; do echo \\\"Hello World $count\\\"; count=$((count+1)); sleep 1; done"));
    }

    public static String defaultSmall() {
        return REPOSITORY + "alpine:3.20.3";
    }

    public static String pickSmall(String contextId) {
        var image = SMALL_IMAGES.poll();
        USED_SMALL_IMAGES.put(contextId, image);
        return image;
    }

    public static void returnSmallIfPresent(String contextId) {
        Optional.ofNullable(USED_SMALL_IMAGES.get(contextId))
                .ifPresent(e -> {
                    SMALL_IMAGES.offer(e);
                    USED_SMALL_IMAGES.remove(contextId);
                });
    }
}
