package com.example.boxwrapper.utils;

import com.example.boxwrapper.config.BoxProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * レート制限マネージャー（Bucket4j使用）
 */
@Slf4j
@Component
public class RateLimiterManager {

    private final BoxProperties boxProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private volatile int currentRequestsPerSecond;
    private final boolean adaptiveEnabled;

    public RateLimiterManager(BoxProperties boxProperties) {
        this.boxProperties = boxProperties;
        this.currentRequestsPerSecond = boxProperties.getRateLimit().getRequestsPerSecond();
        this.adaptiveEnabled = boxProperties.getRateLimit().isAdaptive();
        log.info("RateLimiterManager initialized with {} requests/second, adaptive={}",
            currentRequestsPerSecond, adaptiveEnabled);
    }

    /**
     * APIキーごとのBucketを取得または作成
     */
    public Bucket getBucket(String apiKey) {
        return buckets.computeIfAbsent(apiKey, k -> createBucket());
    }

    /**
     * 新しいBucketを作成
     */
    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
            currentRequestsPerSecond,
            Refill.intervally(currentRequestsPerSecond, Duration.ofSeconds(1))
        );
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * リクエストを試行（許可されればtrue）
     */
    public boolean tryConsume(String apiKey) {
        if (!boxProperties.getRateLimit().isEnabled()) {
            return true;
        }

        Bucket bucket = getBucket(apiKey);
        boolean consumed = bucket.tryConsume(1);

        if (!consumed) {
            log.warn("Rate limit exceeded for API key: {}", maskApiKey(apiKey));
        }

        return consumed;
    }

    /**
     * レート制限に達したときの処理（アダプティブ制御）
     */
    public void handleRateLimitExceeded(String apiKey) {
        if (adaptiveEnabled) {
            decreaseRateLimit();
        }
        log.warn("Rate limit 429 response received for API key: {}", maskApiKey(apiKey));
    }

    /**
     * 成功時の処理（アダプティブ制御）
     */
    public void handleSuccess(String apiKey) {
        if (adaptiveEnabled) {
            increaseRateLimit();
        }
    }

    /**
     * レート制限を下げる
     */
    private synchronized void decreaseRateLimit() {
        int newRate = Math.max(1, (int) (currentRequestsPerSecond * 0.8));
        if (newRate != currentRequestsPerSecond) {
            currentRequestsPerSecond = newRate;
            refreshBuckets();
            log.info("Decreased rate limit to {} requests/second", currentRequestsPerSecond);
        }
    }

    /**
     * レート制限を上げる
     */
    private synchronized void increaseRateLimit() {
        int maxRate = boxProperties.getRateLimit().getRequestsPerSecond();
        if (currentRequestsPerSecond < maxRate) {
            int newRate = Math.min(maxRate, (int) (currentRequestsPerSecond * 1.1));
            if (newRate != currentRequestsPerSecond) {
                currentRequestsPerSecond = newRate;
                refreshBuckets();
                log.info("Increased rate limit to {} requests/second", currentRequestsPerSecond);
            }
        }
    }

    /**
     * 全Bucketを再作成
     */
    private void refreshBuckets() {
        buckets.clear();
    }

    /**
     * 現在のレート制限を取得
     */
    public int getCurrentRateLimit() {
        return currentRequestsPerSecond;
    }

    /**
     * APIキーをマスク
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }
}
