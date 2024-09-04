package io.huskit.gradle.commontest;

import lombok.SneakyThrows;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class BaseTest {

    protected static final int DEFAULT_REPEAT_COUNT = 10;

    protected <T> T parseJson(String json, Class<T> type) {
        return (T) null;
    }

    protected <T> T getJsonField(String json, String field, Class<T> type) {
        return JsonUtil.getJsonField(json, field, type);
    }


    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    @SneakyThrows
    protected void parallel(int nThreads, ThrowingRunnable runnable) {
        var executorService = Executors.newFixedThreadPool(nThreads);
        var readyToStartLock = new CountDownLatch(nThreads);
        var startLock = new CountDownLatch(1);
        var finishedLock = new CountDownLatch(nThreads);

        for (var i = 0; i < nThreads; i++) {
            executorService.submit(() -> {
                try {
                    readyToStartLock.countDown();
                    startLock.await(5, TimeUnit.SECONDS);  // Wait without a timeout
                    runnable.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    finishedLock.countDown();
                }
            });
        }

        readyToStartLock.await();  // Wait for all threads to be ready
        startLock.countDown(); // Signal all threads to start
        finishedLock.await(5, TimeUnit.SECONDS); // Wait for all threads to finish
        executorService.shutdownNow();
    }

    protected void parallel(ThrowingRunnable runnable) {
        parallel(DEFAULT_REPEAT_COUNT, runnable);
    }
}