package io.huskit.containers.http;

import io.huskit.containers.api.container.exec.HtExec;
import io.huskit.containers.cli.CommandResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class HttpExec implements HtExec {

    HtHttpDockerSpec dockerSpec;
    HttpExecSpec httpExecSpec;

    @Override
    public CommandResult exec() {
        return null;
    }
}
