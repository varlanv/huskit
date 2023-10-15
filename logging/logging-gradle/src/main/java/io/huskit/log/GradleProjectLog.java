package io.huskit.log;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GradleProjectLog implements Log {

    private final Logger log;
    private final String path;
    private final String name;

    public GradleProjectLog(Class<?> type, String path, String name) {
        this.log = Logging.getLogger(type);
        this.path = path;
        this.name = name;
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
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " - " + path + " - " + message;
    }
}
