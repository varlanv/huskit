package io.huskit.containers.http;

import io.huskit.common.Log;
import io.huskit.containers.api.image.HtImages;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
final class LocalImagesStash {

    private static final Map<String, Lock> imageToLock = new ConcurrentHashMap<>();
    private static final Set<String> pulledImages = new ConcurrentSkipListSet<>();
    private static final AtomicBoolean isInitialized = new AtomicBoolean();
    private static final Lock initLock = new ReentrantLock();
    HtImages htImages;
    Log log;

    void pullIfAbsent(String image) {
        if (!isInitialized.get()) {
            initLock.lock();
            try {
                if (!isInitialized.get()) {
                    log.info(() -> "Initializing local images stash");
                    var count = new AtomicInteger();
                    htImages.list().stream()
                        .subscribe(stream ->
                            stream.forEach(img -> img.inspect()
                                .tags()
                                .forEach(
                                    imageTag -> {
                                        pulledImages.add(imageTag.repository() + ":" + imageTag.tag());
                                        count.incrementAndGet();
                                    }
                                )
                            ));
                    log.info(() -> "Added [%s] already pulled images to local stash".formatted(count.get()));
                    isInitialized.set(true);
                }
            } finally {
                initLock.unlock();
            }
        }
        if (pulledImages.contains(image)) {
            log.debug(() -> "Image [%s] already pulled".formatted(image));
            return;
        }
        var lock = imageToLock.computeIfAbsent(image, key -> new ReentrantLock());
        lock.lock();
        if (!pulledImages.contains(image)) {
            try {
                var time = System.currentTimeMillis();
                log.debug(() -> "Pulling image from remote - [%s]".formatted(image));
                htImages.pull(image).exec();
                log.debug(() -> "Finished pulling image from remote - [%s] in [%s]ms".formatted(image, System.currentTimeMillis() - time));
                pulledImages.add(image);
            } finally {
                lock.unlock();
            }
        } else {
            lock.unlock();
        }
    }
}
