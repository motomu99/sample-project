package com.example.boxwrapper.controller;

import com.example.boxwrapper.exception.ResourceNotFoundException;
import com.example.boxwrapper.model.response.JobStatusResponse;
import com.example.boxwrapper.utils.JobProgressTracker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ジョブ管理コントローラー.
 *
 * <p>非同期処理ジョブの進捗確認と管理を行うREST APIエンドポイントを提供します。
 * バッチアップロード/ダウンロードなどの長時間実行タスクの状態を確認できます。</p>
 *
 * <p>全てのエンドポイントはAPIキー認証（X-API-Keyヘッダー）が必要です。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Management", description = "非同期ジョブ管理 API")
@SecurityRequirement(name = "API Key")
public class JobController {

    private final JobProgressTracker progressTracker;

    /**
     * ジョブの進捗状況を取得します.
     *
     * <p>ジョブID を指定して、非同期ジョブの現在の状態を確認します。
     * 総アイテム数、完了数、失敗数などの詳細情報が取得できます。</p>
     *
     * @param jobId 確認するジョブのID
     * @return ジョブステータス情報
     * @throws ResourceNotFoundException ジョブが存在しない場合
     */
    @GetMapping("/{jobId}/status")
    @Operation(summary = "ジョブステータス取得", description = "非同期ジョブの進捗状況を取得")
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = "ジョブID", required = true)
            @PathVariable String jobId) {

        JobStatusResponse status = progressTracker.getJobStatus(jobId);

        if (status == null) {
            throw new ResourceNotFoundException("Job", jobId);
        }

        return ResponseEntity.ok(status);
    }

    /**
     * ジョブ情報を削除します（クリーンアップ）.
     *
     * <p>完了したジョブの情報を削除してメモリを解放します。
     * クライアントが結果を受け取った後に呼び出すことを推奨します。</p>
     *
     * @param jobId 削除するジョブのID
     * @return 削除成功時は204 No Content
     */
    @DeleteMapping("/{jobId}")
    @Operation(summary = "ジョブ削除", description = "完了したジョブの情報を削除")
    public ResponseEntity<Void> deleteJob(
            @Parameter(description = "ジョブID", required = true)
            @PathVariable String jobId) {

        progressTracker.removeJob(jobId);

        return ResponseEntity.noContent().build();
    }
}
