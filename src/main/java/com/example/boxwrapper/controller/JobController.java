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
 * ジョブ管理コントローラー
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Management", description = "非同期ジョブ管理 API")
@SecurityRequirement(name = "API Key")
public class JobController {

    private final JobProgressTracker progressTracker;

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

    @DeleteMapping("/{jobId}")
    @Operation(summary = "ジョブ削除", description = "完了したジョブの情報を削除")
    public ResponseEntity<Void> deleteJob(
            @Parameter(description = "ジョブID", required = true)
            @PathVariable String jobId) {

        progressTracker.removeJob(jobId);

        return ResponseEntity.noContent().build();
    }
}
