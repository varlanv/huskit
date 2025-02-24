package io.huskit.gradle.commontest;

import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.io.FileDeleteStrategy;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.setMaxStackTraceElementsDisplayed;

@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public interface BaseTest {

    String DOCKER_TEST_TAG = "docker-test";
    String SLOW_TEST_TAG = "slow-test";
    String FAST_TEST_TAG = "fast-test";
    String UNIT_TEST_TAG = "unit-test";
    String INTEGRATION_TEST_TAG = "integration-test";
    String FUNCTIONAL_TEST_TAG = "functional-test";
    int DEFAULT_REPEAT_COUNT = 10;

    @BeforeAll
    default void setupAssertjParent() {
        setMaxStackTraceElementsDisplayed(Integer.MAX_VALUE);
    }

    default <T> T parseJson(String json, Class<T> type) {
        return (T) null;
    }

    default <T> T getJsonField(String json, String field, Class<T> type) {
        return JsonUtil.getJsonField(json, field, type);
    }

    @SneakyThrows
    default void parallel(int threads, ThrowingRunnable runnable) {
        var executorService = Executors.newFixedThreadPool(threads);
        var exceptionRef = new AtomicReference<Exception>();
        try {
            var readyToStartLock = new CountDownLatch(threads);
            var startLock = new CountDownLatch(1);
            var finishedLock = new CountDownLatch(threads);

            for (var i = 0; i < threads; i++) {
                executorService.submit(() -> {
                    try {
                        readyToStartLock.countDown();
                        startLock.await(5, TimeUnit.SECONDS);  // Wait without a timeout
                        runnable.run();
                    } catch (Exception e) {
                        exceptionRef.set(e);
                    } finally {
                        finishedLock.countDown();
                    }
                });
            }

            readyToStartLock.await();  // Wait for all threads to be ready
            startLock.countDown(); // Signal all threads to start
            finishedLock.await(5, TimeUnit.SECONDS); // Wait for all threads to finish
        } finally {
            executorService.shutdown();
        }
        if (exceptionRef.get() != null) {
            throw exceptionRef.get();
        }
    }

    default void parallel(ThrowingRunnable runnable) {
        parallel(DEFAULT_REPEAT_COUNT, runnable);
    }

    @SneakyThrows
    default File newTempDir() {
        var dir = Files.createTempDirectory("huskitjunit-").toFile();
        dir.deleteOnExit();
        return dir;
    }

    @SneakyThrows
    default Path newTempFile() {
        return Files.createTempFile("huskitjunit-", ".tmp");
    }

    @SneakyThrows
    default void useTempFile(ThrowingConsumer<Path> action) {
        var file = newTempFile();
        try {
            action.accept(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @SneakyThrows
    default <T> T useTempFile(ThrowingFunction<Path, T> action) {
        var file = newTempFile();
        try {
            return action.accept(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    default void runAndDeleteFile(@NonNull File file, ThrowingRunnable runnable) {
        Exception originalException = null;
        try {
            runnable.run();
        } catch (Exception e) {
            originalException = e;
        } finally {
            try {
                FileDeleteStrategy.FORCE.delete(file);
                if (originalException != null) {
                    rethrow(originalException);
                }
            } catch (IOException e) {
                rethrow(Objects.requireNonNullElse(originalException, e));
            }
        }
    }

    default Condition<? super Instant> today() {
        return new Condition<>(instant -> {
            var now = Instant.now();
            var start = now.truncatedTo(TimeUnit.DAYS.toChronoUnit());
            var end = start.plus(1, TimeUnit.DAYS.toChronoUnit());
            return !instant.isBefore(start) && !instant.isAfter(end);
        }, "today");
    }

    default <T extends Throwable> void rethrow(Throwable t) {
        throw hide(t);
    }

    @SuppressWarnings("unchecked")
    default <T extends Throwable> T hide(Throwable t) throws T {
        throw (T) t;
    }

    interface ThrowingRunnable {
        void run() throws Exception;
    }

    interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    interface ThrowingFunction<T, R> {
        R accept(T t) throws Exception;
    }

    @SneakyThrows
    default void microBenchmark(ThrowingRunnable action) {
        microBenchmark(() -> {
            action.run();
            return "";
        });
    }

    @SneakyThrows
    default void microBenchmark(ThrowingSupplier<?> action) {
        microBenchmark(100, "", action);
    }

    @SneakyThrows
    default void microBenchmark(String message, ThrowingRunnable action) {
        microBenchmark(message, () -> {
            action.run();
            return "";
        });
    }

    @SneakyThrows
    default void microBenchmark(String message, ThrowingSupplier<?> action) {
        microBenchmark(100, message, action);
    }

    @SneakyThrows
    default void microBenchmark(Integer iterations, ThrowingSupplier<?> action) {
        microBenchmark(iterations, "", action);
    }

    @SneakyThrows
    default void microBenchmark(Integer iterations, ThrowingRunnable action) {
        microBenchmark(iterations, () -> {
            action.run();
            return "";
        });
    }

    @SneakyThrows
    default void microBenchmark(Integer iterations, String message, ThrowingRunnable action) {
        microBenchmark(iterations, message, () -> {
            action.run();
            return "";
        });
    }

    @SneakyThrows
    default void microBenchmark(Integer iterations, String message, ThrowingSupplier<?> action) {
        var warmupCycles = 50;
        var result = new Object[1];
        for (int i = 0; i < warmupCycles; i++) {
            var r = action.get();
            if (r != null) {
                result[0] = r;
            }
        }
        callGc();
        var totalTime = 0L;
        var totalGcTime = 0L;
        var minTime = 0L;
        var maxTime = 0L;
        var minGcTime = 0L;
        var maxGcTime = 0L;
        for (var i = 0; i < iterations; i++) {
            {
                var nanoGcBefore = System.nanoTime();
//                System.gc();
                var gcTimeTaken = System.nanoTime() - nanoGcBefore;
                totalGcTime = Math.addExact(totalGcTime, gcTimeTaken);
                minGcTime = Math.min(minGcTime, gcTimeTaken);
                maxGcTime = Math.max(maxGcTime, gcTimeTaken);
            }
            {
                var nanosBefore = System.nanoTime();
                var res = action.get();
                var timeTaken = System.nanoTime() - nanosBefore;
                if (res == null) {
                    result[0] = res;
                }
                totalTime = Math.addExact(totalTime, timeTaken);
                minTime = Math.min(minTime, timeTaken);
                maxTime = Math.max(maxTime, timeTaken);
            }
        }
        assertThat(result[0]).matches(it -> true);
        var averageTime = totalTime / iterations;
        var averageGcTime = totalGcTime / iterations;
        Function<Long, String> timeFormat = time -> {
            if (time > 1_000_000_000) { // 1 second
                return Duration.ofNanos(time).toString();
            } else if (time > 10_000_000) { // 10 millis
                return (time / 1_000_000) + " millis";
            } else if (time > 1_000) { // 1 millis
                return (time / 1_000) + " micros";
            } else {
                return time + " nanos";
            }
        };
        System.out.printf("%s%nExecution time: average - %s, total - %s, min - %s, max - %s%n"
                + "GC time: average - %s, total - %s, min - %s, max - %s%n%n",
            message.isEmpty() ? "" : System.lineSeparator() + message,
            timeFormat.apply(averageTime),
            timeFormat.apply(totalTime),
            timeFormat.apply(minTime),
            timeFormat.apply(maxTime),
            timeFormat.apply(averageGcTime),
            timeFormat.apply(totalGcTime),
            timeFormat.apply(minGcTime),
            timeFormat.apply(maxGcTime)
        );
    }

    @SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
    private void callGc() {
        System.gc();
    }
}
