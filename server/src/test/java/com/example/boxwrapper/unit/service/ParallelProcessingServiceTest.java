package com.example.boxwrapper.unit.service;

import com.example.boxwrapper.config.AsyncProperties;
import com.example.boxwrapper.model.request.FileUploadRequest;
import com.example.boxwrapper.model.response.BatchUploadResult;
import com.example.boxwrapper.model.response.FileUploadResponse;
import com.example.boxwrapper.service.BoxFileService;
import com.example.boxwrapper.service.ParallelProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ParallelProcessingServiceのユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParallelProcessingService Unit Tests")
class ParallelProcessingServiceTest {

    @Mock
    private BoxFileService fileService;

    @Mock
    private AsyncProperties asyncProperties;

    @Mock
    private AsyncProperties.Parallel parallelConfig;

    @InjectMocks
    private ParallelProcessingService parallelProcessingService;

    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_FOLDER_ID = "123456";

    @BeforeEach
    void setUp() {
        when(asyncProperties.getParallel()).thenReturn(parallelConfig);
        when(parallelConfig.getMaxConcurrentUploads()).thenReturn(5);
        when(parallelConfig.getMaxConcurrentDownloads()).thenReturn(5);
    }

    @Test
    @DisplayName("uploadFilesParallel - 空のリストで正しく処理されること")
    void testUploadFilesParallel_EmptyList() throws ExecutionException, InterruptedException {
        // Given
        List<FileUploadRequest> requests = new ArrayList<>();

        // When
        CompletableFuture<BatchUploadResult> future =
            parallelProcessingService.uploadFilesParallel(TEST_API_KEY, requests);

        // Then
        BatchUploadResult result = future.get();
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getSuccessful());
        assertEquals(0, result.getFailed());
        assertTrue(result.getSuccessfulFiles().isEmpty());
        assertTrue(result.getFailedFiles().isEmpty());

        verify(fileService, never()).uploadFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("uploadFilesParallel - 単一ファイルのアップロードが成功すること")
    void testUploadFilesParallel_SingleFile() throws ExecutionException, InterruptedException {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "test.txt", "text/plain", "test content".getBytes()
        );

        FileUploadRequest request = new FileUploadRequest();
        request.setFolderId(TEST_FOLDER_ID);
        request.setFile(mockFile);
        request.setFileName("test.txt");

        List<FileUploadRequest> requests = List.of(request);

        FileUploadResponse mockResponse = FileUploadResponse.builder()
            .fileId("file123")
            .fileName("test.txt")
            .fileSize(12L)
            .build();

        when(fileService.uploadFile(TEST_API_KEY, TEST_FOLDER_ID, mockFile))
            .thenReturn(mockResponse);

        // When
        CompletableFuture<BatchUploadResult> future =
            parallelProcessingService.uploadFilesParallel(TEST_API_KEY, requests);

        // Then
        BatchUploadResult result = future.get();
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getSuccessful());
        assertEquals(0, result.getFailed());
        assertEquals(1, result.getSuccessfulFiles().size());
        assertTrue(result.getFailedFiles().isEmpty());

        verify(fileService, times(1)).uploadFile(TEST_API_KEY, TEST_FOLDER_ID, mockFile);
    }

    @Test
    @DisplayName("uploadFilesParallel - 複数ファイルのアップロードが成功すること")
    void testUploadFilesParallel_MultipleFiles() throws ExecutionException, InterruptedException {
        // Given
        List<FileUploadRequest> requests = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            MockMultipartFile mockFile = new MockMultipartFile(
                "file" + i, "test" + i + ".txt", "text/plain", ("content" + i).getBytes()
            );

            FileUploadRequest request = new FileUploadRequest();
            request.setFolderId(TEST_FOLDER_ID);
            request.setFile(mockFile);
            request.setFileName("test" + i + ".txt");
            requests.add(request);

            FileUploadResponse mockResponse = FileUploadResponse.builder()
                .fileId("file" + i)
                .fileName("test" + i + ".txt")
                .fileSize(10L)
                .build();

            when(fileService.uploadFile(eq(TEST_API_KEY), eq(TEST_FOLDER_ID), eq(mockFile)))
                .thenReturn(mockResponse);
        }

        // When
        CompletableFuture<BatchUploadResult> future =
            parallelProcessingService.uploadFilesParallel(TEST_API_KEY, requests);

        // Then
        BatchUploadResult result = future.get();
        assertNotNull(result);
        assertEquals(3, result.getTotal());
        assertEquals(3, result.getSuccessful());
        assertEquals(0, result.getFailed());
        assertEquals(3, result.getSuccessfulFiles().size());
        assertTrue(result.getFailedFiles().isEmpty());

        verify(fileService, times(3)).uploadFile(anyString(), anyString(), any(MultipartFile.class));
    }

    @Test
    @DisplayName("uploadFilesParallel - 一部のファイルが失敗しても他の処理は続行されること")
    void testUploadFilesParallel_PartialFailure() throws ExecutionException, InterruptedException {
        // Given
        List<FileUploadRequest> requests = new ArrayList<>();

        // First file - success
        MockMultipartFile mockFile1 = new MockMultipartFile(
            "file1", "test1.txt", "text/plain", "content1".getBytes()
        );
        FileUploadRequest request1 = new FileUploadRequest();
        request1.setFolderId(TEST_FOLDER_ID);
        request1.setFile(mockFile1);
        request1.setFileName("test1.txt");
        requests.add(request1);

        // Second file - failure
        MockMultipartFile mockFile2 = new MockMultipartFile(
            "file2", "test2.txt", "text/plain", "content2".getBytes()
        );
        FileUploadRequest request2 = new FileUploadRequest();
        request2.setFolderId(TEST_FOLDER_ID);
        request2.setFile(mockFile2);
        request2.setFileName("test2.txt");
        requests.add(request2);

        // Third file - success
        MockMultipartFile mockFile3 = new MockMultipartFile(
            "file3", "test3.txt", "text/plain", "content3".getBytes()
        );
        FileUploadRequest request3 = new FileUploadRequest();
        request3.setFolderId(TEST_FOLDER_ID);
        request3.setFile(mockFile3);
        request3.setFileName("test3.txt");
        requests.add(request3);

        FileUploadResponse mockResponse1 = FileUploadResponse.builder()
            .fileId("file1").fileName("test1.txt").fileSize(10L).build();
        FileUploadResponse mockResponse3 = FileUploadResponse.builder()
            .fileId("file3").fileName("test3.txt").fileSize(10L).build();

        when(fileService.uploadFile(TEST_API_KEY, TEST_FOLDER_ID, mockFile1))
            .thenReturn(mockResponse1);
        when(fileService.uploadFile(TEST_API_KEY, TEST_FOLDER_ID, mockFile2))
            .thenThrow(new RuntimeException("Upload failed"));
        when(fileService.uploadFile(TEST_API_KEY, TEST_FOLDER_ID, mockFile3))
            .thenReturn(mockResponse3);

        // When
        CompletableFuture<BatchUploadResult> future =
            parallelProcessingService.uploadFilesParallel(TEST_API_KEY, requests);

        // Then
        BatchUploadResult result = future.get();
        assertNotNull(result);
        assertEquals(3, result.getTotal());
        assertEquals(2, result.getSuccessful());
        assertEquals(1, result.getFailed());
        assertEquals(2, result.getSuccessfulFiles().size());
        assertEquals(1, result.getFailedFiles().size());
        assertEquals("test2.txt", result.getFailedFiles().get(0).getFileName());
    }

    @Test
    @DisplayName("uploadFilesParallel - 全てのファイルが失敗すること")
    void testUploadFilesParallel_AllFailed() throws ExecutionException, InterruptedException {
        // Given
        List<FileUploadRequest> requests = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            MockMultipartFile mockFile = new MockMultipartFile(
                "file" + i, "test" + i + ".txt", "text/plain", ("content" + i).getBytes()
            );

            FileUploadRequest request = new FileUploadRequest();
            request.setFolderId(TEST_FOLDER_ID);
            request.setFile(mockFile);
            request.setFileName("test" + i + ".txt");
            requests.add(request);

            when(fileService.uploadFile(eq(TEST_API_KEY), eq(TEST_FOLDER_ID), eq(mockFile)))
                .thenThrow(new RuntimeException("Upload failed for file" + i));
        }

        // When
        CompletableFuture<BatchUploadResult> future =
            parallelProcessingService.uploadFilesParallel(TEST_API_KEY, requests);

        // Then
        BatchUploadResult result = future.get();
        assertNotNull(result);
        assertEquals(3, result.getTotal());
        assertEquals(0, result.getSuccessful());
        assertEquals(3, result.getFailed());
        assertTrue(result.getSuccessfulFiles().isEmpty());
        assertEquals(3, result.getFailedFiles().size());
    }

    @Test
    @DisplayName("downloadFilesParallel - 空のリストで正しく処理されること")
    void testDownloadFilesParallel_EmptyList() throws ExecutionException, InterruptedException {
        // Given
        List<String> fileIds = new ArrayList<>();

        // When
        CompletableFuture<List<byte[]>> future =
            parallelProcessingService.downloadFilesParallel(TEST_API_KEY, fileIds);

        // Then
        List<byte[]> results = future.get();
        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(fileService, never()).downloadFile(anyString(), anyString());
    }

    @Test
    @DisplayName("downloadFilesParallel - 単一ファイルのダウンロードが成功すること")
    void testDownloadFilesParallel_SingleFile() throws ExecutionException, InterruptedException {
        // Given
        String fileId = "file123";
        List<String> fileIds = List.of(fileId);
        byte[] mockContent = "test content".getBytes();

        when(fileService.downloadFile(TEST_API_KEY, fileId))
            .thenReturn(mockContent);

        // When
        CompletableFuture<List<byte[]>> future =
            parallelProcessingService.downloadFilesParallel(TEST_API_KEY, fileIds);

        // Then
        List<byte[]> results = future.get();
        assertNotNull(results);
        assertEquals(1, results.size());
        assertArrayEquals(mockContent, results.get(0));

        verify(fileService, times(1)).downloadFile(TEST_API_KEY, fileId);
    }

    @Test
    @DisplayName("downloadFilesParallel - 複数ファイルのダウンロードが成功すること")
    void testDownloadFilesParallel_MultipleFiles() throws ExecutionException, InterruptedException {
        // Given
        List<String> fileIds = List.of("file1", "file2", "file3");

        when(fileService.downloadFile(TEST_API_KEY, "file1"))
            .thenReturn("content1".getBytes());
        when(fileService.downloadFile(TEST_API_KEY, "file2"))
            .thenReturn("content2".getBytes());
        when(fileService.downloadFile(TEST_API_KEY, "file3"))
            .thenReturn("content3".getBytes());

        // When
        CompletableFuture<List<byte[]>> future =
            parallelProcessingService.downloadFilesParallel(TEST_API_KEY, fileIds);

        // Then
        List<byte[]> results = future.get();
        assertNotNull(results);
        assertEquals(3, results.size());
        assertArrayEquals("content1".getBytes(), results.get(0));
        assertArrayEquals("content2".getBytes(), results.get(1));
        assertArrayEquals("content3".getBytes(), results.get(2));

        verify(fileService, times(3)).downloadFile(anyString(), anyString());
    }

    @Test
    @DisplayName("downloadFilesParallel - 一部のファイルが失敗した場合、失敗したファイルにnullが設定されること")
    void testDownloadFilesParallel_PartialFailure() throws ExecutionException, InterruptedException {
        // Given
        List<String> fileIds = List.of("file1", "file2", "file3");

        when(fileService.downloadFile(TEST_API_KEY, "file1"))
            .thenReturn("content1".getBytes());
        when(fileService.downloadFile(TEST_API_KEY, "file2"))
            .thenThrow(new RuntimeException("Download failed"));
        when(fileService.downloadFile(TEST_API_KEY, "file3"))
            .thenReturn("content3".getBytes());

        // When
        CompletableFuture<List<byte[]>> future =
            parallelProcessingService.downloadFilesParallel(TEST_API_KEY, fileIds);

        // Then
        List<byte[]> results = future.get();
        assertNotNull(results);
        assertEquals(3, results.size());
        assertArrayEquals("content1".getBytes(), results.get(0));
        assertNull(results.get(1));
        assertArrayEquals("content3".getBytes(), results.get(2));
    }

    @Test
    @DisplayName("並行数設定が正しく適用されること - アップロード")
    void testMaxConcurrentUploads_Configuration() {
        // Given
        when(parallelConfig.getMaxConcurrentUploads()).thenReturn(3);

        List<FileUploadRequest> requests = new ArrayList<>();
        MockMultipartFile mockFile = new MockMultipartFile(
            "file", "test.txt", "text/plain", "content".getBytes()
        );
        FileUploadRequest request = new FileUploadRequest();
        request.setFolderId(TEST_FOLDER_ID);
        request.setFile(mockFile);
        request.setFileName("test.txt");
        requests.add(request);

        // When
        parallelProcessingService.uploadFilesParallel(TEST_API_KEY, requests);

        // Then
        verify(parallelConfig, atLeastOnce()).getMaxConcurrentUploads();
    }

    @Test
    @DisplayName("並行数設定が正しく適用されること - ダウンロード")
    void testMaxConcurrentDownloads_Configuration() {
        // Given
        when(parallelConfig.getMaxConcurrentDownloads()).thenReturn(3);
        List<String> fileIds = List.of("file1");

        // When
        parallelProcessingService.downloadFilesParallel(TEST_API_KEY, fileIds);

        // Then
        verify(parallelConfig, atLeastOnce()).getMaxConcurrentDownloads();
    }

    @Test
    @DisplayName("BatchUploadResult - 成功/失敗の統計が正しく計算されること")
    void testBatchUploadResult_Statistics() {
        // Given
        BatchUploadResult result = BatchUploadResult.builder()
            .total(10)
            .successful(7)
            .failed(3)
            .successfulFiles(new ArrayList<>())
            .failedFiles(new ArrayList<>())
            .build();

        // Then
        assertEquals(10, result.getTotal());
        assertEquals(7, result.getSuccessful());
        assertEquals(3, result.getFailed());
    }
}
