package com.example.boxwrapper.service;

import com.example.boxwrapper.config.AsyncProperties;
import com.example.boxwrapper.model.request.FileUploadRequest;
import com.example.boxwrapper.model.response.BatchUploadResult;
import com.example.boxwrapper.model.response.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 並列処理サービス
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParallelProcessingService {

    private final BoxFileService fileService;
    private final AsyncProperties asyncProperties;

    /**
     * 複数ファイルを並列アップロード
     */
    @Async
    public CompletableFuture<BatchUploadResult> uploadFilesParallel(
            String apiKey,
            List<FileUploadRequest> requests) {

        int maxConcurrent = asyncProperties.getParallel().getMaxConcurrentUploads();
        Semaphore semaphore = new Semaphore(maxConcurrent);

        log.info("Starting parallel upload of {} files with max concurrency {}",
            requests.size(), maxConcurrent);

        List<CompletableFuture<FileUploadResponse>> futures = new ArrayList<>();
        List<FileUploadRequest> failedRequests = new ArrayList<>();

        for (FileUploadRequest request : requests) {
            CompletableFuture<FileUploadResponse> future = CompletableFuture.supplyAsync(() -> {
                try {
                    semaphore.acquire();
                    try {
                        return fileService.uploadFile(apiKey, request.getFolderId(), request.getFile());
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("アップロードが中断されました", e);
                } catch (Exception e) {
                    log.error("Failed to upload file: {}", request.getFileName(), e);
                    throw new RuntimeException(e);
                }
            });

            futures.add(future);
        }

        // Wait for all uploads to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        return allFutures.thenApply(v -> {
            List<FileUploadResponse> successful = new ArrayList<>();
            List<BatchUploadResult.FailedFileInfo> failed = new ArrayList<>();

            for (int i = 0; i < futures.size(); i++) {
                try {
                    FileUploadResponse response = futures.get(i).get();
                    successful.add(response);
                } catch (Exception e) {
                    FileUploadRequest request = requests.get(i);
                    failed.add(BatchUploadResult.FailedFileInfo.builder()
                        .fileName(request.getFileName())
                        .errorMessage(e.getMessage())
                        .build());
                }
            }

            log.info("Parallel upload completed: {}/{} successful, {} failed",
                successful.size(), requests.size(), failed.size());

            return BatchUploadResult.builder()
                .total(requests.size())
                .successful(successful.size())
                .failed(failed.size())
                .successfulFiles(successful)
                .failedFiles(failed)
                .build();
        });
    }

    /**
     * 複数ファイルを並列ダウンロード
     */
    @Async
    public CompletableFuture<List<byte[]>> downloadFilesParallel(
            String apiKey,
            List<String> fileIds) {

        int maxConcurrent = asyncProperties.getParallel().getMaxConcurrentDownloads();
        Semaphore semaphore = new Semaphore(maxConcurrent);

        log.info("Starting parallel download of {} files with max concurrency {}",
            fileIds.size(), maxConcurrent);

        List<CompletableFuture<byte[]>> futures = new ArrayList<>();

        for (String fileId : fileIds) {
            CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> {
                try {
                    semaphore.acquire();
                    try {
                        return fileService.downloadFile(apiKey, fileId);
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("ダウンロードが中断されました", e);
                }
            });

            futures.add(future);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        return allFutures.thenApply(v -> {
            List<byte[]> results = new ArrayList<>();
            for (CompletableFuture<byte[]> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    log.error("Failed to download file", e);
                    results.add(null);
                }
            }
            return results;
        });
    }
}
