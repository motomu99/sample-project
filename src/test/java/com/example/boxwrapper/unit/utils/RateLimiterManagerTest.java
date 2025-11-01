package com.example.boxwrapper.unit.utils;

import com.example.boxwrapper.config.BoxProperties;
import com.example.boxwrapper.utils.RateLimiterManager;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RateLimiterManagerのユニットテスト.
 *
 * <p>レート制限、アダプティブ制御、スレッドセーフティをテストします。</p>
 */
@DisplayName("RateLimiterManager Unit Tests")
class RateLimiterManagerTest {

    private BoxProperties boxProperties;
    private RateLimiterManager rateLimiterManager;

    @BeforeEach
    void setUp() {
        boxProperties = new BoxProperties();
        BoxProperties.RateLimit rateLimit = new BoxProperties.RateLimit();
        rateLimit.setEnabled(true);
        rateLimit.setRequestsPerSecond(10);
        rateLimit.setAdaptive(true);
        boxProperties.setRateLimit(rateLimit);
    }

    @Test
    @DisplayName("初期化 - デフォルト設定でRateLimiterManagerが正しく初期化されること")
    void testInitialization_DefaultSettings() {
        // When
        rateLimiterManager = new RateLimiterManager(boxProperties);

        // Then
        assertEquals(10, rateLimiterManager.getCurrentRateLimit());
    }

    @Test
    @DisplayName("getBucket - 同じAPIキーに対して同じBucketインスタンスを返すこと")
    void testGetBucket_SameInstanceForSameApiKey() {
        // Given
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key-123";

        // When
        Bucket bucket1 = rateLimiterManager.getBucket(apiKey);
        Bucket bucket2 = rateLimiterManager.getBucket(apiKey);

        // Then
        assertNotNull(bucket1);
        assertSame(bucket1, bucket2);
    }

    @Test
    @DisplayName("getBucket - 異なるAPIキーに対して異なるBucketインスタンスを返すこと")
    void testGetBucket_DifferentInstancesForDifferentApiKeys() {
        // Given
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey1 = "test-api-key-1";
        String apiKey2 = "test-api-key-2";

        // When
        Bucket bucket1 = rateLimiterManager.getBucket(apiKey1);
        Bucket bucket2 = rateLimiterManager.getBucket(apiKey2);

        // Then
        assertNotNull(bucket1);
        assertNotNull(bucket2);
        assertNotSame(bucket1, bucket2);
    }

    @Test
    @DisplayName("tryConsume - レート制限が無効の場合、常にtrueを返すこと")
    void testTryConsume_RateLimitDisabled() {
        // Given
        boxProperties.getRateLimit().setEnabled(false);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";

        // When & Then
        // 大量のリクエストでも全てtrueを返す
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimiterManager.tryConsume(apiKey));
        }
    }

    @Test
    @DisplayName("tryConsume - レート制限が有効の場合、設定されたレート以下のリクエストが成功すること")
    void testTryConsume_RateLimitEnabled_WithinLimit() {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(5);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";

        // When & Then
        // 設定されたレート以下のリクエストは成功する
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterManager.tryConsume(apiKey),
                "Request " + (i + 1) + " should succeed");
        }
    }

    @Test
    @DisplayName("tryConsume - レート制限が有効の場合、設定されたレートを超えるリクエストが失敗すること")
    void testTryConsume_RateLimitEnabled_ExceedsLimit() {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(5);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";

        // When
        // 設定されたレート分のリクエストを消費
        for (int i = 0; i < 5; i++) {
            rateLimiterManager.tryConsume(apiKey);
        }

        // Then
        // 追加のリクエストは失敗する
        assertFalse(rateLimiterManager.tryConsume(apiKey),
            "Request exceeding rate limit should fail");
    }

    @Test
    @DisplayName("handleRateLimitExceeded - アダプティブ制御が有効の場合、レート制限が減少すること")
    void testHandleRateLimitExceeded_AdaptiveEnabled() {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(10);
        boxProperties.getRateLimit().setAdaptive(true);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";
        int initialRate = rateLimiterManager.getCurrentRateLimit();

        // When
        rateLimiterManager.handleRateLimitExceeded(apiKey);

        // Then
        int newRate = rateLimiterManager.getCurrentRateLimit();
        assertTrue(newRate < initialRate,
            "Rate should decrease after 429 error. Initial: " + initialRate + ", New: " + newRate);
        assertEquals(8, newRate, "Rate should be 80% of initial rate");
    }

    @Test
    @DisplayName("handleRateLimitExceeded - アダプティブ制御が無効の場合、レート制限が変わらないこと")
    void testHandleRateLimitExceeded_AdaptiveDisabled() {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(10);
        boxProperties.getRateLimit().setAdaptive(false);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";
        int initialRate = rateLimiterManager.getCurrentRateLimit();

        // When
        rateLimiterManager.handleRateLimitExceeded(apiKey);

        // Then
        int newRate = rateLimiterManager.getCurrentRateLimit();
        assertEquals(initialRate, newRate,
            "Rate should not change when adaptive control is disabled");
    }

    @Test
    @DisplayName("handleSuccess - アダプティブ制御が有効の場合、レート制限が増加すること")
    void testHandleSuccess_AdaptiveEnabled() {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(10);
        boxProperties.getRateLimit().setAdaptive(true);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";

        // まずレート制限を減少させる
        rateLimiterManager.handleRateLimitExceeded(apiKey);
        int decreasedRate = rateLimiterManager.getCurrentRateLimit();

        // When
        rateLimiterManager.handleSuccess(apiKey);

        // Then
        int newRate = rateLimiterManager.getCurrentRateLimit();
        assertTrue(newRate > decreasedRate,
            "Rate should increase after success. Decreased: " + decreasedRate + ", New: " + newRate);
    }

    @Test
    @DisplayName("handleSuccess - レート制限が最大値に達している場合、それ以上増加しないこと")
    void testHandleSuccess_MaxRateNotExceeded() {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(10);
        boxProperties.getRateLimit().setAdaptive(true);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";
        int maxRate = boxProperties.getRateLimit().getRequestsPerSecond();

        // When
        // 既に最大レートなので、成功しても増加しない
        rateLimiterManager.handleSuccess(apiKey);

        // Then
        int newRate = rateLimiterManager.getCurrentRateLimit();
        assertEquals(maxRate, newRate,
            "Rate should not exceed max rate");
    }

    @Test
    @DisplayName("decreaseRateLimit - 複数回の減少で最小値1になること")
    void testDecreaseRateLimit_MultipleDecreasesToMinimum() {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(10);
        boxProperties.getRateLimit().setAdaptive(true);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";

        // When
        // レート制限を複数回減少させる
        for (int i = 0; i < 20; i++) {
            rateLimiterManager.handleRateLimitExceeded(apiKey);
        }

        // Then
        int finalRate = rateLimiterManager.getCurrentRateLimit();
        assertTrue(finalRate >= 1,
            "Rate should not go below 1. Final rate: " + finalRate);
    }

    @Test
    @DisplayName("increaseRateLimit - 減少後に成功を繰り返すと元のレートに戻ること")
    void testIncreaseRateLimit_RecoveryToOriginalRate() {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(10);
        boxProperties.getRateLimit().setAdaptive(true);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";
        int originalRate = rateLimiterManager.getCurrentRateLimit();

        // When
        // レート制限を減少させる
        rateLimiterManager.handleRateLimitExceeded(apiKey);

        // 成功を繰り返してレートを回復させる
        for (int i = 0; i < 20; i++) {
            rateLimiterManager.handleSuccess(apiKey);
        }

        // Then
        int finalRate = rateLimiterManager.getCurrentRateLimit();
        assertEquals(originalRate, finalRate,
            "Rate should recover to original rate after multiple successes");
    }

    @Test
    @DisplayName("並行処理 - 複数のスレッドから同時にtryConsumeを呼び出しても正しく動作すること")
    void testConcurrentTryConsume() throws InterruptedException {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(100);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";
        int numThreads = 10;
        int requestsPerThread = 10;

        // When
        int[] successCount = {0};
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    if (rateLimiterManager.tryConsume(apiKey)) {
                        synchronized (successCount) {
                            successCount[0]++;
                        }
                    }
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        // レート制限内のリクエストが成功すること
        assertTrue(successCount[0] <= 100,
            "Success count should not exceed rate limit. Actual: " + successCount[0]);
        assertTrue(successCount[0] > 0,
            "At least some requests should succeed");
    }

    @Test
    @DisplayName("並行処理 - 複数のスレッドから同時にhandleRateLimitExceededを呼び出しても正しく動作すること")
    void testConcurrentHandleRateLimitExceeded() throws InterruptedException {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(100);
        boxProperties.getRateLimit().setAdaptive(true);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";
        int numThreads = 10;

        // When
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                rateLimiterManager.handleRateLimitExceeded(apiKey);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        // レート制限が減少していること
        int finalRate = rateLimiterManager.getCurrentRateLimit();
        assertTrue(finalRate < 100,
            "Rate should decrease after concurrent 429 errors. Final rate: " + finalRate);
        assertTrue(finalRate >= 1,
            "Rate should not go below 1");
    }

    @Test
    @DisplayName("複数のAPIキーで独立してレート制限が管理されること")
    void testMultipleApiKeys_IndependentRateLimiting() {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(5);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey1 = "test-api-key-1";
        String apiKey2 = "test-api-key-2";

        // When
        // apiKey1で制限まで消費
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterManager.tryConsume(apiKey1));
        }

        // Then
        // apiKey1は制限に達する
        assertFalse(rateLimiterManager.tryConsume(apiKey1));

        // apiKey2はまだ使える
        assertTrue(rateLimiterManager.tryConsume(apiKey2));
    }

    @Test
    @DisplayName("getCurrentRateLimit - 現在のレート制限を正しく取得できること")
    void testGetCurrentRateLimit() {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(15);
        rateLimiterManager = new RateLimiterManager(boxProperties);

        // When
        int currentRate = rateLimiterManager.getCurrentRateLimit();

        // Then
        assertEquals(15, currentRate);
    }

    @Test
    @DisplayName("アダプティブ制御のシナリオテスト - 429エラーと成功が混在する場合")
    void testAdaptiveControlScenario() {
        // Given
        boxProperties.getRateLimit().setRequestsPerSecond(10);
        boxProperties.getRateLimit().setAdaptive(true);
        rateLimiterManager = new RateLimiterManager(boxProperties);
        String apiKey = "test-api-key";

        // When & Then
        // 初期レート
        assertEquals(10, rateLimiterManager.getCurrentRateLimit());

        // 429エラー発生でレート減少
        rateLimiterManager.handleRateLimitExceeded(apiKey);
        assertEquals(8, rateLimiterManager.getCurrentRateLimit());

        // 再度429エラー発生でさらに減少
        rateLimiterManager.handleRateLimitExceeded(apiKey);
        assertEquals(6, rateLimiterManager.getCurrentRateLimit());

        // 成功でレート増加
        rateLimiterManager.handleSuccess(apiKey);
        assertEquals(6, rateLimiterManager.getCurrentRateLimit()); // 6 * 1.1 = 6 (小数点切り捨て)

        // 複数回成功でレート回復
        for (int i = 0; i < 10; i++) {
            rateLimiterManager.handleSuccess(apiKey);
        }
        assertEquals(10, rateLimiterManager.getCurrentRateLimit());
    }
}
