package io.huskit.log;

public interface Log {

    void info(String message);

    void info(String message, Object... args);

    void lifecycle(String message);

    void lifecycle(String message, Object... args);

    void error(String var1);

    void error(String var1, Object var2);

    void error(String var1, Object var2, Object var3);
}
