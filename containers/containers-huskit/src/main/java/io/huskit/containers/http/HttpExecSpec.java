package io.huskit.containers.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HttpExecSpec implements HtUrl {

    String containerId;
    Map<String, Object> body;

    public HttpExecSpec(CharSequence containerId, CharSequence command, Iterable<? extends CharSequence> args) {
        this.body = new HashMap<>();
        this.containerId = containerId.toString();
        this.body.put("AttachStdin", false);
        this.body.put("AttachStdout", false);
        this.body.put("AttachStderr", false);
        this.body.put("Tty", false);
        var cmd = new ArrayList<String>();
        cmd.add(command.toString());
        for (var arg : args) {
            cmd.add(arg.toString());
        }
        this.body.put("Cmd", cmd);
    }

    @Override
    public String url() {
        return "/containers/" + containerId + "/exec";
    }

    @Override
    public Map<String, Object> body() {
        return body;
    }
}
