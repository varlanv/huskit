package io.huskit.log.fake;

import io.huskit.log.Log;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FakeLog implements Log {

    private final List<FakeLoggedMessage> loggedMessages;

    public FakeLog() {
        this(new CopyOnWriteArrayList<>());
    }

    @Override
    public void info(String message) {
        loggedMessages.add(new FakeLoggedMessage(message, "info"));
    }

    @Override
    public void info(String message, Object... args) {
        loggedMessages.add(new FakeLoggedMessage(message, toStringList(args), "info"));
    }

    @Override
    public void lifecycle(String message) {
        loggedMessages.add(new FakeLoggedMessage(message, "lifecycle"));
    }

    @Override
    public void lifecycle(String message, Object... args) {
        loggedMessages.add(new FakeLoggedMessage(message, toStringList(args), "lifecycle"));
    }

    @Override
    public void error(String var1) {
        loggedMessages.add(new FakeLoggedMessage(var1, "error"));
    }

    @Override
    public void error(String var1, Object var2) {
        loggedMessages.add(new FakeLoggedMessage(var1, List.of(String.valueOf(var2)), "error"));
    }

    @Override
    public void error(String var1, Object var2, Object var3) {
        loggedMessages.add(new FakeLoggedMessage(var1, List.of(String.valueOf(var2), String.valueOf(var3)), "error"));
    }

    public List<FakeLoggedMessage> loggedMessages() {
        return new ArrayList<>(loggedMessages);
    }

    private List<String> toStringList(Object[] args) {
        return Arrays.asList(args).stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }
}
