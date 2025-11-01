package com.example.boxwrapper.utils;

import com.example.boxwrapper.model.response.JobStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 非同期ジョブの進捗トラッキング
 */
@Slf4j
@Component
public class JobProgressTracker {

    private final Map<String, JobProgress> jobs = new ConcurrentHashMap<>();

    /**
     * 新しいジョブを作成
     */
    public String createJob(int totalItems) {
        String jobId = UUID.randomUUID().toString();
        JobProgress progress = new JobProgress(jobId, totalItems);
        jobs.put(jobId, progress);
        log.info("Created job: {} with {} items", jobId, totalItems);
        return jobId;
    }

    /**
     * ジョブの進捗を更新（成功）
     */
    public void updateSuccess(String jobId) {
        JobProgress progress = jobs.get(jobId);
        if (progress != null) {
            progress.incrementCompleted();
        }
    }

    /**
     * ジョブの進捗を更新（失敗）
     */
    public void updateFailure(String jobId, String errorMessage) {
        JobProgress progress = jobs.get(jobId);
        if (progress != null) {
            progress.incrementFailed();
            progress.setErrorMessage(errorMessage);
        }
    }

    /**
     * ジョブを完了状態にする
     */
    public void completeJob(String jobId) {
        JobProgress progress = jobs.get(jobId);
        if (progress != null) {
            progress.setStatus("COMPLETED");
            progress.setCompletedAt(LocalDateTime.now());
            log.info("Job {} completed: {}/{} successful, {} failed",
                jobId, progress.getCompleted(), progress.getTotal(), progress.getFailed());
        }
    }

    /**
     * ジョブを失敗状態にする
     */
    public void failJob(String jobId, String errorMessage) {
        JobProgress progress = jobs.get(jobId);
        if (progress != null) {
            progress.setStatus("FAILED");
            progress.setErrorMessage(errorMessage);
            progress.setCompletedAt(LocalDateTime.now());
            log.error("Job {} failed: {}", jobId, errorMessage);
        }
    }

    /**
     * ジョブのステータスを取得
     */
    public JobStatusResponse getJobStatus(String jobId) {
        JobProgress progress = jobs.get(jobId);
        if (progress == null) {
            return null;
        }

        return JobStatusResponse.builder()
            .jobId(progress.getJobId())
            .status(progress.getStatus())
            .total(progress.getTotal())
            .completed(progress.getCompleted())
            .failed(progress.getFailed())
            .startedAt(progress.getStartedAt())
            .completedAt(progress.getCompletedAt())
            .errorMessage(progress.getErrorMessage())
            .build();
    }

    /**
     * ジョブを削除（クリーンアップ）
     */
    public void removeJob(String jobId) {
        jobs.remove(jobId);
        log.debug("Removed job: {}", jobId);
    }

    /**
     * 内部進捗管理クラス
     */
    @lombok.Data
    private static class JobProgress {
        private final String jobId;
        private final int total;
        private int completed = 0;
        private int failed = 0;
        private String status = "IN_PROGRESS";
        private final LocalDateTime startedAt = LocalDateTime.now();
        private LocalDateTime completedAt;
        private String errorMessage;

        public JobProgress(String jobId, int total) {
            this.jobId = jobId;
            this.total = total;
        }

        public synchronized void incrementCompleted() {
            this.completed++;
        }

        public synchronized void incrementFailed() {
            this.failed++;
        }
    }
}
