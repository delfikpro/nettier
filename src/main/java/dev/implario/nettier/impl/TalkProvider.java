package dev.implario.nettier.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import dev.implario.nettier.NettierRemote;
import dev.implario.nettier.Talk;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@RequiredArgsConstructor
public class TalkProvider {

    private final NettierRemote remote;

    private final Cache<Long, Talk> talkCache = CacheBuilder.newBuilder()
            .concurrencyLevel(3)
            .expireAfterWrite(50L, TimeUnit.SECONDS)
            .removalListener(this::onCacheRemoval)
            .build();

    private final AtomicLong packetCounter = new AtomicLong();

    private void onCacheRemoval(RemovalNotification<Long, Talk> notification) {
        if (notification.getCause() == RemovalCause.EXPIRED) {
            val talk = notification.getValue();
            if (talk == null) return;

            CompletableFuture<Object> callback = talk.getFuture();
            if (callback != null && !callback.isDone()) {
                callback.completeExceptionally(new TimeoutException("Talk " + notification.getKey() + " timed out"));
            }
        }
    }

    @SneakyThrows
    public Talk provideTalk(long talkId) {
        return talkId == 0 ? new Talk(talkId, remote.getNode(), remote) :
                talkCache.get(talkId, () -> new Talk(talkId, remote.getNode(), remote));
    }


}
