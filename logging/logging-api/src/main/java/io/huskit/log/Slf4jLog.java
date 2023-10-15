package io.huskit.log;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Slf4jLog implements Log {

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void info(String message, Object... args) {
        log.info(message, args);
    }

    @Override
    public void lifecycle(String message) {
        info(message);
    }

    @Override
    public void lifecycle(String message, Object... args) {
        info(message, args);
    }

    @Override
    public void error(String var1) {
        log.error(var1);
    }

    @Override
    public void error(String var1, Object var2) {
        log.error(var1, var2);
    }

    @Override
    public void error(String var1, Object var2, Object var3) {
        log.error(var1, var2, var3);
    }
}
