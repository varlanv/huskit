package io.huskit.containers.http;

import io.huskit.containers.api.container.run.HtCreateSpec;
import io.huskit.containers.api.image.HtImgName;

import java.util.*;
import java.util.stream.Collectors;

class HttpCreateSpec implements HtCreateSpec, HtUrl {

    HtImgName image;
    Map<String, Object> body;

    public HttpCreateSpec(HtImgName image) {
        this.image = image;
        this.body = new HashMap<>();
    }

    @Override
    public HttpCreateSpec withLabels(Map<String, ?> labels) {
        if (!labels.isEmpty()) {
            var stringLabels = new HashMap<String, String>(labels.size());
            for (var entry : labels.entrySet()) {
                var value = entry.getValue();
                if (value == null) {
                    throw new IllegalArgumentException("Label value cannot be null");
                }
                stringLabels.put(entry.getKey(), value.toString());
            }
            body.put("Labels", stringLabels);
        }
        return this;
    }

    @Override
    public HttpCreateSpec withEnv(Map<String, ?> env) {
        if (!env.isEmpty()) {
            body.put(
                    "Env",
                    env.entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .collect(Collectors.toList())
            );
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public HttpCreateSpec withRemove() {
        var hostConfig = body.get("HostConfig");
        if (hostConfig == null) {
            var c = new HashMap<String, Object>();
            c.put("AutoRemove", true);
            body.put("HostConfig", c);
            return this;
        }
        var c = (Map<String, Object>) hostConfig;
        c.put("AutoRemove", true);
        return this;
    }

    @Override
    public HttpCreateSpec withPortBinding(Number hostPort, Number containerPort) {
        return withPortBindings(Map.of(containerPort, hostPort));
    }

    @Override
    public HttpCreateSpec withPortBindings(Map<? extends Number, ? extends Number> portBindings) {
        var pb = new HashMap<String, Object>();
        portBindings.forEach((hostPort, containerPort) ->
                pb.put(containerPort.toString() + "/tcp", List.of(Map.of("HostPort", hostPort.toString()))));
        var hostConfig = new HashMap<String, Object>();
        hostConfig.put("PortBindings", pb);
        body.put("HostConfig", hostConfig);
        var exposedPorts = new HashMap<String, Object>();
        portBindings.forEach((containerPort, hostPort) -> exposedPorts.put(containerPort.toString() + "/tcp", new HashMap<>()));
        body.put("ExposedPorts", exposedPorts);
        return this;
    }

    @Override
    public HttpCreateSpec withCommand(CharSequence command, Object... args) {
        return withCommand(command, Arrays.asList(args));
    }

    @Override
    public HttpCreateSpec withCommand(CharSequence command, Iterable<?> args) {
        var cmd = new ArrayList<String>();
        cmd.add(command.toString());
        for (var arg : args) {
            cmd.add(arg.toString());
        }
        body.put("Cmd", cmd);
        return this;
    }

    @Override
    public String url() {
        return "/containers/create";
    }

    @Override
    public Map<String, Object> body() {
        this.body.put("Image", image.reference());
        return this.body;
    }
}
