package com.example.boxwrapper.unit.utils;

import com.example.boxwrapper.model.response.JobStatusResponse;
import com.example.boxwrapper.utils.JobProgressTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JobProgressTrackerのユニットテスト.
 *
 * <p>ジョブの作成、進捗更新、ステータス取得、削除の各機能をテストします。</p>
 */
@DisplayName("JobProgressTracker Unit Tests")
class JobProgressTrackerTest {

    private JobProgressTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new JobProgressTracker();
    }

    @Test
    @DisplayName("createJob - 正常にジョブを作成してUUID形式のジョブIDを返すこと")
    void testCreateJob_Success() {
        // Given
        int totalItems = 100;

        // When
        String jobId = tracker.createJob(totalItems);

        // Then
        assertNotNull(jobId);
        assertFalse(jobId.isEmpty());
        // UUID形式の検証（8-4-4-4-12のハイフン区切り）
        assertTrue(jobId.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }

    @Test
    @DisplayName("createJob - 作成直後のジョブステータスがIN_PROGRESSであること")
    void testCreateJob_InitialStatus() {
        // Given
        int totalItems = 50;

        // When
        String jobId = tracker.createJob(totalItems);
        JobStatusResponse status = tracker.getJobStatus(jobId);

        // Then
        assertNotNull(status);
        assertEquals(jobId, status.getJobId());
        assertEquals("IN_PROGRESS", status.getStatus());
        assertEquals(totalItems, status.getTotal());
        assertEquals(0, status.getCompleted());
        assertEquals(0, status.getFailed());
        assertNotNull(status.getStartedAt());
        assertNull(status.getCompletedAt());
        assertNull(status.getErrorMessage());
    }

    @Test
    @DisplayName("updateSuccess - 成功カウントが正しくインクリメントされること")
    void testUpdateSuccess_IncrementCompleted() {
        // Given
        String jobId = tracker.createJob(10);

        // When
        tracker.updateSuccess(jobId);
        tracker.updateSuccess(jobId);
        tracker.updateSuccess(jobId);

        // Then
        JobStatusResponse status = tracker.getJobStatus(jobId);
        assertEquals(3, status.getCompleted());
        assertEquals(0, status.getFailed());
    }

    @Test
    @DisplayName("updateSuccess - 存在しないジョブIDでも例外がスローされないこと")
    void testUpdateSuccess_NonExistentJob() {
        // Given
        String nonExistentJobId = "non-existent-job-id";

        // When & Then
        assertDoesNotThrow(() -> tracker.updateSuccess(nonExistentJobId));
    }

    @Test
    @DisplayName("updateFailure - 失敗カウントとエラーメッセージが正しく記録されること")
    void testUpdateFailure_IncrementFailedAndSetErrorMessage() {
        // Given
        String jobId = tracker.createJob(10);
        String errorMessage = "File upload failed due to network error";

        // When
        tracker.updateFailure(jobId, errorMessage);
        tracker.updateFailure(jobId, "Another error");

        // Then
        JobStatusResponse status = tracker.getJobStatus(jobId);
        assertEquals(2, status.getFailed());
        assertEquals("Another error", status.getErrorMessage()); // 最後のエラーメッセージが保持される
    }

    @Test
    @DisplayName("updateFailure - 存在しないジョブIDでも例外がスローされないこと")
    void testUpdateFailure_NonExistentJob() {
        // Given
        String nonExistentJobId = "non-existent-job-id";

        // When & Then
        assertDoesNotThrow(() -> tracker.updateFailure(nonExistentJobId, "error"));
    }

    @Test
    @DisplayName("completeJob - ステータスがCOMPLETEDに変更され完了時刻が記録されること")
    void testCompleteJob_StatusChangedToCompleted() {
        // Given
        String jobId = tracker.createJob(5);
        tracker.updateSuccess(jobId);
        tracker.updateSuccess(jobId);
        tracker.updateSuccess(jobId);

        // When
        tracker.completeJob(jobId);

        // Then
        JobStatusResponse status = tracker.getJobStatus(jobId);
        assertEquals("COMPLETED", status.getStatus());
        assertNotNull(status.getCompletedAt());
        assertEquals(3, status.getCompleted());
    }

    @Test
    @DisplayName("completeJob - 存在しないジョブIDでも例外がスローされないこと")
    void testCompleteJob_NonExistentJob() {
        // Given
        String nonExistentJobId = "non-existent-job-id";

        // When & Then
        assertDoesNotThrow(() -> tracker.completeJob(nonExistentJobId));
    }

    @Test
    @DisplayName("failJob - ステータスがFAILEDに変更されエラーメッセージが記録されること")
    void testFailJob_StatusChangedToFailedWithError() {
        // Given
        String jobId = tracker.createJob(10);
        tracker.updateSuccess(jobId);
        String errorMessage = "Fatal error occurred during batch processing";

        // When
        tracker.failJob(jobId, errorMessage);

        // Then
        JobStatusResponse status = tracker.getJobStatus(jobId);
        assertEquals("FAILED", status.getStatus());
        assertEquals(errorMessage, status.getErrorMessage());
        assertNotNull(status.getCompletedAt());
        assertEquals(1, status.getCompleted());
    }

    @Test
    @DisplayName("failJob - 存在しないジョブIDでも例外がスローされないこと")
    void testFailJob_NonExistentJob() {
        // Given
        String nonExistentJobId = "non-existent-job-id";

        // When & Then
        assertDoesNotThrow(() -> tracker.failJob(nonExistentJobId, "error"));
    }

    @Test
    @DisplayName("getJobStatus - 存在しないジョブIDの場合nullを返すこと")
    void testGetJobStatus_NonExistentJob() {
        // Given
        String nonExistentJobId = "non-existent-job-id";

        // When
        JobStatusResponse status = tracker.getJobStatus(nonExistentJobId);

        // Then
        assertNull(status);
    }

    @Test
    @DisplayName("removeJob - ジョブが正常に削除されること")
    void testRemoveJob_Success() {
        // Given
        String jobId = tracker.createJob(10);
        assertNotNull(tracker.getJobStatus(jobId));

        // When
        tracker.removeJob(jobId);

        // Then
        JobStatusResponse status = tracker.getJobStatus(jobId);
        assertNull(status);
    }

    @Test
    @DisplayName("removeJob - 存在しないジョブIDでも例外がスローされないこと")
    void testRemoveJob_NonExistentJob() {
        // Given
        String nonExistentJobId = "non-existent-job-id";

        // When & Then
        assertDoesNotThrow(() -> tracker.removeJob(nonExistentJobId));
    }

    @Test
    @DisplayName("並行処理 - 複数のスレッドから同時にupdateSuccessを呼び出しても正しくカウントされること")
    void testConcurrentUpdateSuccess() throws InterruptedException {
        // Given
        String jobId = tracker.createJob(1000);
        int numThreads = 10;
        int updatesPerThread = 10;

        // When
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < updatesPerThread; j++) {
                    tracker.updateSuccess(jobId);
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        JobStatusResponse status = tracker.getJobStatus(jobId);
        assertEquals(numThreads * updatesPerThread, status.getCompleted());
    }

    @Test
    @DisplayName("並行処理 - 複数のスレッドから同時にupdateFailureを呼び出しても正しくカウントされること")
    void testConcurrentUpdateFailure() throws InterruptedException {
        // Given
        String jobId = tracker.createJob(1000);
        int numThreads = 10;
        int updatesPerThread = 10;

        // When
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            int threadNum = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < updatesPerThread; j++) {
                    tracker.updateFailure(jobId, "Error from thread " + threadNum);
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        JobStatusResponse status = tracker.getJobStatus(jobId);
        assertEquals(numThreads * updatesPerThread, status.getFailed());
        assertNotNull(status.getErrorMessage());
    }

    @Test
    @DisplayName("複合シナリオ - 成功と失敗が混在する場合の正しいカウント")
    void testMixedSuccessAndFailure() {
        // Given
        String jobId = tracker.createJob(20);

        // When
        tracker.updateSuccess(jobId);
        tracker.updateSuccess(jobId);
        tracker.updateSuccess(jobId);
        tracker.updateFailure(jobId, "Error 1");
        tracker.updateSuccess(jobId);
        tracker.updateFailure(jobId, "Error 2");

        // Then
        JobStatusResponse status = tracker.getJobStatus(jobId);
        assertEquals(4, status.getCompleted());
        assertEquals(2, status.getFailed());
        assertEquals("Error 2", status.getErrorMessage());
        assertEquals("IN_PROGRESS", status.getStatus());
    }

    @Test
    @DisplayName("ゼロアイテムのジョブが作成できること")
    void testCreateJob_ZeroItems() {
        // Given
        int totalItems = 0;

        // When
        String jobId = tracker.createJob(totalItems);
        JobStatusResponse status = tracker.getJobStatus(jobId);

        // Then
        assertNotNull(jobId);
        assertEquals(0, status.getTotal());
        assertEquals("IN_PROGRESS", status.getStatus());
    }
}
