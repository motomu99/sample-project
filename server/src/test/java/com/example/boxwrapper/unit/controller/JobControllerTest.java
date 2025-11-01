package com.example.boxwrapper.unit.controller;

import com.example.boxwrapper.controller.JobController;
import com.example.boxwrapper.exception.ResourceNotFoundException;
import com.example.boxwrapper.model.response.JobStatusResponse;
import com.example.boxwrapper.utils.JobProgressTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * JobControllerのユニットテスト.
 */
@WebMvcTest(JobController.class)
@DisplayName("JobController Unit Tests")
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobProgressTracker progressTracker;

    private static final String API_KEY = "test-api-key-123";
    private static final String TEST_JOB_ID = "job-123-456-789";

    private JobStatusResponse mockJobStatus;

    @BeforeEach
    void setUp() {
        mockJobStatus = JobStatusResponse.builder()
            .jobId(TEST_JOB_ID)
            .status("IN_PROGRESS")
            .total(100)
            .completed(50)
            .failed(5)
            .startedAt(LocalDateTime.now().minusMinutes(10))
            .completedAt(null)
            .errorMessage(null)
            .build();
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/status - ジョブステータス取得が成功すること")
    void testGetJobStatus_Success() throws Exception {
        // Given
        when(progressTracker.getJobStatus(TEST_JOB_ID))
            .thenReturn(mockJobStatus);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/{jobId}/status", TEST_JOB_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(TEST_JOB_ID))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.total").value(100))
            .andExpect(jsonPath("$.completed").value(50))
            .andExpect(jsonPath("$.failed").value(5));

        verify(progressTracker, times(1)).getJobStatus(TEST_JOB_ID);
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/status - ジョブが存在しない場合、404エラーが返ること")
    void testGetJobStatus_NotFound() throws Exception {
        // Given
        when(progressTracker.getJobStatus(TEST_JOB_ID))
            .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/{jobId}/status", TEST_JOB_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isNotFound());

        verify(progressTracker, times(1)).getJobStatus(TEST_JOB_ID);
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/status - 完了済みジョブのステータスが正しく返ること")
    void testGetJobStatus_CompletedJob() throws Exception {
        // Given
        JobStatusResponse completedJob = JobStatusResponse.builder()
            .jobId(TEST_JOB_ID)
            .status("COMPLETED")
            .total(100)
            .completed(95)
            .failed(5)
            .startedAt(LocalDateTime.now().minusMinutes(30))
            .completedAt(LocalDateTime.now())
            .errorMessage(null)
            .build();

        when(progressTracker.getJobStatus(TEST_JOB_ID))
            .thenReturn(completedJob);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/{jobId}/status", TEST_JOB_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.completed").value(95))
            .andExpect(jsonPath("$.failed").value(5))
            .andExpect(jsonPath("$.completedAt").exists());

        verify(progressTracker, times(1)).getJobStatus(TEST_JOB_ID);
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/status - 失敗したジョブのステータスが正しく返ること")
    void testGetJobStatus_FailedJob() throws Exception {
        // Given
        JobStatusResponse failedJob = JobStatusResponse.builder()
            .jobId(TEST_JOB_ID)
            .status("FAILED")
            .total(100)
            .completed(30)
            .failed(70)
            .startedAt(LocalDateTime.now().minusMinutes(15))
            .completedAt(LocalDateTime.now())
            .errorMessage("Batch processing failed due to rate limit exceeded")
            .build();

        when(progressTracker.getJobStatus(TEST_JOB_ID))
            .thenReturn(failedJob);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/{jobId}/status", TEST_JOB_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.errorMessage").value("Batch processing failed due to rate limit exceeded"))
            .andExpect(jsonPath("$.completedAt").exists());

        verify(progressTracker, times(1)).getJobStatus(TEST_JOB_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/jobs/{jobId} - ジョブ削除が成功すること")
    void testDeleteJob_Success() throws Exception {
        // Given
        doNothing().when(progressTracker).removeJob(TEST_JOB_ID);

        // When & Then
        mockMvc.perform(delete("/api/v1/jobs/{jobId}", TEST_JOB_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isNoContent());

        verify(progressTracker, times(1)).removeJob(TEST_JOB_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/jobs/{jobId} - 存在しないジョブの削除でも成功すること")
    void testDeleteJob_NonExistentJob() throws Exception {
        // Given
        doNothing().when(progressTracker).removeJob(anyString());

        // When & Then
        mockMvc.perform(delete("/api/v1/jobs/{jobId}", "non-existent-job")
                .header("X-API-Key", API_KEY))
            .andExpect(status().isNoContent());

        verify(progressTracker, times(1)).removeJob("non-existent-job");
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{jobId}/status - APIキーがない場合、401エラーが返ること")
    void testGetJobStatus_MissingApiKey() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/jobs/{jobId}/status", TEST_JOB_ID))
            .andExpect(status().isUnauthorized());

        verify(progressTracker, never()).getJobStatus(anyString());
    }

    @Test
    @DisplayName("DELETE /api/v1/jobs/{jobId} - APIキーがない場合、401エラーが返ること")
    void testDeleteJob_MissingApiKey() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/jobs/{jobId}", TEST_JOB_ID))
            .andExpect(status().isUnauthorized());

        verify(progressTracker, never()).removeJob(anyString());
    }

    @Test
    @DisplayName("複数のジョブステータス取得が正しく動作すること")
    void testGetJobStatus_MultipleJobs() throws Exception {
        // Given
        String jobId1 = "job-1";
        String jobId2 = "job-2";

        JobStatusResponse job1Status = JobStatusResponse.builder()
            .jobId(jobId1).status("IN_PROGRESS").total(50).completed(25).failed(0)
            .startedAt(LocalDateTime.now()).build();

        JobStatusResponse job2Status = JobStatusResponse.builder()
            .jobId(jobId2).status("COMPLETED").total(100).completed(100).failed(0)
            .startedAt(LocalDateTime.now().minusMinutes(5))
            .completedAt(LocalDateTime.now()).build();

        when(progressTracker.getJobStatus(jobId1)).thenReturn(job1Status);
        when(progressTracker.getJobStatus(jobId2)).thenReturn(job2Status);

        // When & Then - Job 1
        mockMvc.perform(get("/api/v1/jobs/{jobId}/status", jobId1)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(jobId1))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // When & Then - Job 2
        mockMvc.perform(get("/api/v1/jobs/{jobId}/status", jobId2)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value(jobId2))
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(progressTracker, times(1)).getJobStatus(jobId1);
        verify(progressTracker, times(1)).getJobStatus(jobId2);
    }

    @Test
    @DisplayName("ジョブステータスのエラーメッセージがnullの場合も正しく処理されること")
    void testGetJobStatus_NullErrorMessage() throws Exception {
        // Given
        JobStatusResponse statusWithoutError = JobStatusResponse.builder()
            .jobId(TEST_JOB_ID)
            .status("COMPLETED")
            .total(50)
            .completed(50)
            .failed(0)
            .startedAt(LocalDateTime.now().minusMinutes(10))
            .completedAt(LocalDateTime.now())
            .errorMessage(null)
            .build();

        when(progressTracker.getJobStatus(TEST_JOB_ID))
            .thenReturn(statusWithoutError);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/{jobId}/status", TEST_JOB_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errorMessage").doesNotExist());

        verify(progressTracker, times(1)).getJobStatus(TEST_JOB_ID);
    }

    @Test
    @DisplayName("進行中のジョブの進捗率が正しく表示されること")
    void testGetJobStatus_ProgressPercentage() throws Exception {
        // Given
        JobStatusResponse progressJob = JobStatusResponse.builder()
            .jobId(TEST_JOB_ID)
            .status("IN_PROGRESS")
            .total(200)
            .completed(150)
            .failed(10)
            .startedAt(LocalDateTime.now().minusMinutes(20))
            .build();

        when(progressTracker.getJobStatus(TEST_JOB_ID))
            .thenReturn(progressJob);

        // When & Then
        mockMvc.perform(get("/api/v1/jobs/{jobId}/status", TEST_JOB_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(200))
            .andExpect(jsonPath("$.completed").value(150))
            .andExpect(jsonPath("$.failed").value(10));
        // Progress: (150 + 10) / 200 = 80% processed

        verify(progressTracker, times(1)).getJobStatus(TEST_JOB_ID);
    }
}
