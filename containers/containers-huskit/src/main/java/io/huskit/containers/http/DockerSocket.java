package io.huskit.containers.http;

import io.huskit.common.Mutable;
import io.huskit.common.function.ThrowingFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public interface DockerSocket {

    <T> Http.Response<T> send(Request<T> request);

    <T> CompletableFuture<Http.Response<T>> sendAsync(Request<T> request);

    void close();

    class Request<T> {

        @Getter
        Http.Request http;
        @Getter
        ThrowingFunction<Npipe.HttpFlow, List<T>> action;
        Mutable<RepeatRead> repeatReadPredicate;
        Mutable<ExpectedStatus> expectedStatus;

        public Request(Http.Request http,
                       ThrowingFunction<Npipe.HttpFlow, List<T>> action) {
            this.http = http;
            this.action = action;
            this.repeatReadPredicate = Mutable.of();
            this.expectedStatus = Mutable.of();
        }

        public Request<T> withRepeatReadPredicate(Predicate<String> predicate, Duration backoff) {
            repeatReadPredicate.set(new RepeatRead(predicate, backoff));
            return this;
        }

        public Request<T> withExpectedStatus(Integer status) {
            expectedStatus.set(new ExpectedStatus(status));
            return this;
        }

        public Optional<RepeatRead> repeatReadPredicate() {
            return repeatReadPredicate.maybe();
        }

        public Optional<ExpectedStatus> expectedStatus() {
            return expectedStatus.maybe();
        }

        @Getter
        @RequiredArgsConstructor
        public static class ExpectedStatus {

            Integer status;
        }

        @Getter
        @RequiredArgsConstructor
        public static class RepeatRead {

            Predicate<String> predicate;
            Duration backoff;
        }
    }
}
