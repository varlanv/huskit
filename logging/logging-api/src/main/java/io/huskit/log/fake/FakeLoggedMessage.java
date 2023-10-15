package io.huskit.log.fake;

import lombok.Value;

import java.util.List;

@Value
public class FakeLoggedMessage {

    String message;
    List<String> args;
    String level;

    public FakeLoggedMessage(String message, List<String> args, String level) {
        this.message = message;
        this.args = args;
        this.level = level;
    }

    public FakeLoggedMessage(String message, String level) {
        this(message, List.of(), level);
    }
}
