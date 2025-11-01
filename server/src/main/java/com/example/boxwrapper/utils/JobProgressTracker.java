package com.example.boxwrapper.utils;

import com.example.boxwrapper.model.response.JobStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 非同期ジョブの進捗トラッキング.
 *
 * <p>バッチ処理や長時間実行される非同期タスクの進捗状況を管理します。
 * ジョブIDを発行し、進捗（成功/失敗カウント）を追跡し、ステータスを提供します。</p>
 *
 * <p>スレッドセーフな実装で、複数の非同期タスクから同時にアクセス可能です。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class JobProgressTracker {

    private final Map<String, JobProgress> jobs = new ConcurrentHashMap<>();

    /**
     * 新しいジョブを作成し、一意のジョブIDを返します.
     *
     * <p>作成されたジョブは初期状態（IN_PROGRESS）で登録され、
     * 進捗管理が開始されます。</p>
     *
     * @param totalItems ジョブで処理する総アイテム数
     * @return 生成されたジョブID（UUID形式）
     */
    public String createJob(int totalItems) {
        String jobId = UUID.randomUUID().toString();
        JobProgress progress = new JobProgress(jobId, totalItems);
        jobs.put(jobId, progress);
        log.info("Created job: {} with {} items", jobId, totalItems);
        return jobId;
    }

    /**
     * ジョブの成功カウントをインクリメントします.
     *
     * <p>1つのアイテムが正常に処理された際に呼び出します。</p>
     *
     * @param jobId 対象のジョブID
     */
    public void updateSuccess(String jobId) {
        JobProgress progress = jobs.get(jobId);
        if (progress != null) {
            progress.incrementCompleted();
        }
    }

    /**
     * ジョブの失敗カウントをインクリメントし、エラーメッセージを記録します.
     *
     * <p>1つのアイテムの処理が失敗した際に呼び出します。</p>
     *
     * @param jobId 対象のジョブID
     * @param errorMessage 失敗の詳細メッセージ
     */
    public void updateFailure(String jobId, String errorMessage) {
        JobProgress progress = jobs.get(jobId);
        if (progress != null) {
            progress.incrementFailed();
            progress.setErrorMessage(errorMessage);
        }
    }

    /**
     * ジョブを完了状態に設定します.
     *
     * <p>全てのアイテムの処理が終了した際に呼び出します。
     * ステータスが「COMPLETED」に変更され、完了時刻が記録されます。</p>
     *
     * @param jobId 対象のジョブID
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
     * ジョブを失敗状態に設定します.
     *
     * <p>ジョブ全体が失敗した場合（致命的エラーなど）に呼び出します。
     * ステータスが「FAILED」に変更され、エラーメッセージと完了時刻が記録されます。</p>
     *
     * @param jobId 対象のジョブID
     * @param errorMessage ジョブ失敗の理由
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
     * ジョブの現在のステータスを取得します.
     *
     * <p>ジョブの進捗状況（総数、完了数、失敗数）、ステータス、
     * 開始/完了時刻などの詳細情報を含むレスポンスを返します。</p>
     *
     * @param jobId 対象のジョブID
     * @return ジョブステータス情報。ジョブが存在しない場合はnull
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
     * ジョブ情報を削除します（クリーンアップ）.
     *
     * <p>完了したジョブの情報を破棄してメモリを解放します。
     * クライアントが結果を受け取った後に呼び出すことを推奨します。</p>
     *
     * @param jobId 削除するジョブID
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
