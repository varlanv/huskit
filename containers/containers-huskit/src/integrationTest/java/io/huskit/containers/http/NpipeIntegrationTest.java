package io.huskit.containers.http;

import io.huskit.common.function.ThrowingSupplier;
import io.huskit.gradle.commontest.DockerIntegrationTest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Disabled
@EnabledOnOs(OS.WINDOWS)
public class NpipeIntegrationTest implements DockerIntegrationTest {

    String dockerNpipe = "\\\\.\\pipe\\docker_engine";

    @Test
    void async_file_channel_raw() throws Exception {
        var request = "GET /containers/json?all=true HTTP/1.1\r\nHost: localhost\r\n\r\n";
        try (var channel = AsynchronousFileChannel.open(
                Paths.get(dockerNpipe),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            ThrowingRunnable r = () -> {
                var time = System.currentTimeMillis();
                channel.write(ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8)), 0).get();
                var buffer = ByteBuffer.allocate(4096);
                channel.read(buffer, 0).get();
                buffer.flip();
                var decode = StandardCharsets.UTF_8.decode(buffer).toString();
                var lines = Arrays.asList(decode.split("\r\n"));
                var headers = new HashMap<String, String>();
                for (int i = 0; i < lines.size(); i++) {
                    if (i == 0) {
                        continue;
                    }
                    var line = lines.get(i);
                    String[] split = line.split(":");
                    if (split.length == 2) {
                        headers.put(split[0].trim(), split[1].trim());
                    }
                    if (line.isEmpty()) {
                        break;
                    }
                }
//                var list = new JSONArray(lines.get(lines.size() - 1)).toList();
//                System.out.println(list);
                System.out.println("Time: " + Duration.ofMillis(System.currentTimeMillis() - time));
            };
            r.run();
            r.run();
        }
    }

    @Test
    void async_file_channel_raw_log_follow() throws Exception {
        var containerId = "c22b114e40ebd3de59593a11181bdddd490c886a60b20efe9be174d9b8f3d49b";
        var request = "GET " + "/containers/" + containerId + "/logs?stdout=true&stderr=true&follow=true HTTP/1.1\r\nHost: localhost\r\nConnection: keep-alive\r\n\r\n";
        try (var channel = AsynchronousFileChannel.open(
                Paths.get(dockerNpipe),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            ThrowingSupplier<String> readOnce = () -> {
                var buffer = ByteBuffer.allocate(16512);
                channel.read(buffer, 0).get();
                buffer.flip();
                return StandardCharsets.UTF_8.decode(buffer).toString();
            };
            ThrowingRunnable r = () -> {
                var time = System.currentTimeMillis();
                channel.write(ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8)), 0).get();
                var buffer = ByteBuffer.allocate(16512);
                channel.read(buffer, 0).get();
                buffer.flip();
                int i = 0;
//                readOnce.get();
//                readOnce.get();
//                System.out.println("Time: " + Duration.ofMillis(System.currentTimeMillis() - time));
                while (true) {
                    var data = readOnce.get();
                    System.out.println("ITERATING -> " + i++ + " -> " + data);
                }
            };
            r.run();
        }
    }

    @Test
    void async_file_channel_raw1() throws Exception {
        var createRequest = "POST /containers/create HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Connection: keep-alive\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: 141\r\n" +
                "\r\n" +
                "{\"Image\":\"public.ecr.aws/docker/library/alpine:3.20.3\",\"Cmd\":[\"sh\",\"-c\",\"echo 'Hello World 1' && echo 'Hello World 2' && tail -f /dev/null\"]}";
        var startRequest = "POST /containers/%s/start HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Connection: keep-alive\r\n" +
                "Content-Type: application/json\r\n" +
                "\r\n";
        var logsRequest = "GET /containers/%s/logs?stdout=true&stderr=true HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "Connection: keep-alive\r\n" +
                "\r\n";
        try (var channel = AsynchronousFileChannel.open(
                Paths.get(dockerNpipe),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            ThrowingRunnable r = () -> {
                var time = System.currentTimeMillis();
                channel.write(ByteBuffer.wrap(createRequest.getBytes(StandardCharsets.UTF_8)), 0).get();
                var buffer = ByteBuffer.allocate(16536);
                channel.read(buffer, 0).get();
                buffer.flip();
                var decode1 = StandardCharsets.UTF_8.decode(buffer).toString();
                buffer.clear();
                channel.read(buffer, 0).get();
                buffer.flip();
                var decode2 = StandardCharsets.UTF_8.decode(buffer).toString();
//                System.out.println(decode1);
//                System.out.println(decode2);
                var jsonObject = new JSONObject(decode2.split("\r\n")[1]);
                var id = jsonObject.getString("Id");
                channel.write(ByteBuffer.wrap(String.format(startRequest, id).getBytes(StandardCharsets.UTF_8)), 0).get();
                buffer.clear();
                channel.read(buffer, 0).get();
                buffer.flip();
                var decode3 = StandardCharsets.UTF_8.decode(buffer).toString();
//                System.out.println(decode3);
                channel.write(ByteBuffer.wrap(String.format(logsRequest, id).getBytes(StandardCharsets.UTF_8)), 0).get();
                buffer.clear();
                channel.read(buffer, 0).get();
                buffer.flip();
                var decode4 = StandardCharsets.UTF_8.decode(buffer).toString();
                System.out.println(decode4);
                buffer.clear();
                channel.read(buffer, 0).get();
                buffer.flip();
                var decode5 = StandardCharsets.UTF_8.decode(buffer).toString();
                System.out.println(decode5);
            };
            r.run();
        }
    }

    @Test
    void file_channel_raw() throws Exception {
        var request = "GET /containers/json?all=true HTTP/1.1\r\nHost: localhost\r\n\r\n";
        try (var channel = FileChannel.open(
                Paths.get(dockerNpipe),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8)), 0);
            var buffer = ByteBuffer.allocate(4096);
            channel.read(buffer, 0);
            buffer.flip();
            var decode = StandardCharsets.UTF_8.decode(buffer).toString();
            System.out.println(decode);
        }
    }

    @Test
    @Timeout(3)
    void containers_json() throws Exception {
        var httpRequests = new HttpRequests();
        var subject = new Npipe(dockerNpipe);
        try {
            ThrowingRunnable r = () -> {
                var before = System.currentTimeMillis();
                subject.sendAsync(
                                new DockerSocket.Request<>(
                                        httpRequests.get(HtUrl.of("/containers/json?all=true")),
                                        httpFlow -> new JSONArray(
                                                new JSONTokener(
                                                        httpFlow.reader()
                                                )
                                        ).toList()
                                )
                        )
                        .thenAccept(it -> System.out.println(it.body().list()))
                        .whenComplete((it, e) -> System.out.println("Time: " + (System.currentTimeMillis() - before) + "ms"))
                        .join();
            };
            r.run();
            r.run();
        } finally {
            subject.close();
        }
    }

    @Test
    void t() {
        AtomicInteger integer = new AtomicInteger(0);
        Stream.generate(() -> {
//                    try {
//                        Thread.sleep(1000);
                    return integer.incrementAndGet();
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
                })
                .takeWhile(i -> i <= 10)
                .flatMap(i -> Stream.of("A", "B", "C").map(s -> s + i))
                .forEach(System.out::println);
    }
}
