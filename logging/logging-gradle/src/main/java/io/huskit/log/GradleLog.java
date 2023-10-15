package io.huskit.log;

import lombok.RequiredArgsConstructor;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.text.SimpleDateFormat;
import java.util.Date;

@RequiredArgsConstructor
public class GradleLog implements Log {

    private final Logger log;

    public GradleLog(Class<?> type) {
        this.log = Logging.getLogger(type);
    }

    @Override
    public void info(String message) {
        log.lifecycle(formatted(message));
    }

    @Override
    public void info(String message, Object... args) {
        log.lifecycle(formatted(message), args);
    }

    @Override
    public void lifecycle(String message) {
        log.lifecycle(formatted(message));
    }

    @Override
    public void lifecycle(String message, Object... args) {
        log.lifecycle(formatted(message), args);
    }

    @Override
    public void error(String var1) {
        log.error(formatted(var1));
    }

    @Override
    public void error(String var1, Object var2) {
        log.error(formatted(var1), var2);
    }

    @Override
    public void error(String var1, Object var2, Object var3) {
        log.error(formatted(var1), var2, var3);
    }

    private String formatted(String message) {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " - " + message;
    }
}
