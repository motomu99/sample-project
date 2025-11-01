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
 * 並列処理サービス.
 *
 * <p>複数のファイルを並列にアップロード/ダウンロードする機能を提供します。
 * セマフォによる同時実行数制御により、レート制限を考慮した安全な並列処理を実現します。</p>
 *
 * <p>並列度の設定：
 * <ul>
 *   <li>最大同時アップロード数: 5（設定変更可能）</li>
 *   <li>最大同時ダウンロード数: 5（設定変更可能）</li>
 * </ul>
 * </p>
 *
 * <p>部分的な失敗でも処理を続行し、成功/失敗の詳細情報を返します。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParallelProcessingService {

    private final BoxFileService fileService;
    private final AsyncProperties asyncProperties;

    /**
     * 複数のファイルを並列アップロードします.
     *
     * <p>指定されたファイルリストを並列にアップロードし、
     * 全ての処理が完了するまで待機します。セマフォにより、
     * 設定された最大同時アップロード数を超えないよう制御されます。</p>
     *
     * <p>一部のファイルがアップロード失敗しても、他のファイルの処理は続行されます。
     * 結果には成功/失敗したファイルの詳細情報が含まれます。</p>
     *
     * @param apiKey 認証用のAPIキー
     * @param requests アップロードするファイルのリクエストリスト
     * @return 非同期処理結果（成功数、失敗数、詳細情報を含む）
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
     * 複数のファイルを並列ダウンロードします.
     *
     * <p>指定されたファイルIDリストのファイルを並列にダウンロードし、
     * 全ての処理が完了するまで待機します。セマフォにより、
     * 設定された最大同時ダウンロード数を超えないよう制御されます。</p>
     *
     * <p>失敗したファイルに対してはnullが返されます。</p>
     *
     * @param apiKey 認証用のAPIキー
     * @param fileIds ダウンロードするファイルのIDリスト
     * @return 非同期処理結果（ファイル内容のバイト配列リスト、失敗時はnull）
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
