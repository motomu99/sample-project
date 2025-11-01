package com.example.boxwrapper.unit.service;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFile;
import com.box.sdk.BoxFolder;
import com.example.boxwrapper.client.BoxClientManager;
import com.example.boxwrapper.exception.BoxApiException;
import com.example.boxwrapper.exception.ResourceNotFoundException;
import com.example.boxwrapper.model.response.FileInfoResponse;
import com.example.boxwrapper.model.response.FileUploadResponse;
import com.example.boxwrapper.service.BoxFileService;
import com.example.boxwrapper.utils.RateLimiterManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoxFileService - Unit Tests")
class BoxFileServiceTest {

    private static final String API_KEY = "test-api-key";
    private static final String FOLDER_ID = "123456";
    private static final String FILE_ID = "789012";

    @Mock
    private BoxClientManager clientManager;

    @Mock
    private RateLimiterManager rateLimiterManager;

    @Mock
    private BoxAPIConnection apiConnection;

    private BoxFileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new BoxFileService(clientManager, rateLimiterManager);
        lenient().when(clientManager.getConnection(API_KEY)).thenReturn(apiConnection);
    }

    @Test
    @DisplayName("uploadFile - 正常にアップロードできること")
    void uploadFile_Success() throws Exception {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        MockMultipartFile multipartFile = new MockMultipartFile(
            "file",
            "sample.txt",
            "text/plain",
            "box wrapper".getBytes()
        );

        BoxFile.Info uploadedInfo = mock(BoxFile.Info.class);
        Date createdAt = Date.from(Instant.parse("2024-01-01T10:00:00Z"));
        when(uploadedInfo.getID()).thenReturn(FILE_ID);
        when(uploadedInfo.getName()).thenReturn("sample.txt");
        when(uploadedInfo.getSize()).thenReturn(multipartFile.getSize());
        when(uploadedInfo.getCreatedAt()).thenReturn(createdAt);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> when(mock.uploadFile(any(InputStream.class), eq("sample.txt"))).thenReturn(uploadedInfo)
        )) {
            FileUploadResponse response = fileService.uploadFile(API_KEY, FOLDER_ID, multipartFile);

            assertThat(response.getFileId()).isEqualTo(FILE_ID);
            assertThat(response.getFileName()).isEqualTo("sample.txt");
            assertThat(response.getSize()).isEqualTo(multipartFile.getSize());
            assertThat(response.getCreatedAt()).isNotNull();

            verify(rateLimiterManager).handleSuccess(API_KEY);
            assertThat(mockedFolder.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("uploadFile - レート制限で即座に失敗すること")
    void uploadFile_RateLimitGateRejected() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(false);

        MockMultipartFile multipartFile = new MockMultipartFile(
            "file",
            "sample.txt",
            "text/plain",
            "box wrapper".getBytes()
        );

        assertThatThrownBy(() -> fileService.uploadFile(API_KEY, FOLDER_ID, multipartFile))
            .isInstanceOf(BoxApiException.class)
            .hasMessageContaining("レート制限");

        verify(clientManager, never()).getConnection(API_KEY);
        verify(rateLimiterManager, never()).handleSuccess(API_KEY);
    }

    @Test
    @DisplayName("uploadFile - Box APIが429を返した際にハンドリングされること")
    void uploadFile_BoxApi429Handled() throws Exception {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        MockMultipartFile multipartFile = new MockMultipartFile(
            "file",
            "sample.txt",
            "text/plain",
            "box wrapper".getBytes()
        );

        BoxAPIException rateLimitException = mock(BoxAPIException.class);
        when(rateLimitException.getResponseCode()).thenReturn(429);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> doThrow(rateLimitException)
                .when(mock).uploadFile(any(InputStream.class), eq("sample.txt"))
        )) {
            assertThatThrownBy(() -> fileService.uploadFile(API_KEY, FOLDER_ID, multipartFile))
                .isInstanceOf(BoxApiException.class)
                .hasMessageContaining("ファイルアップロードに失敗しました");

            verify(rateLimiterManager).handleRateLimitExceeded(API_KEY);
            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
            assertThat(mockedFolder.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("getFileInfo - ファイル情報を取得できること")
    void getFileInfo_Success() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxFile.Info fileInfo = mock(BoxFile.Info.class);
        BoxFolder.Info parentInfo = mock(BoxFolder.Info.class);
        Date createdAt = Date.from(Instant.parse("2024-02-01T08:30:00Z"));
        Date modifiedAt = Date.from(Instant.parse("2024-02-02T12:00:00Z"));

        when(fileInfo.getID()).thenReturn(FILE_ID);
        when(fileInfo.getName()).thenReturn("sample.txt");
        when(fileInfo.getSize()).thenReturn(1024L);
        when(fileInfo.getParent()).thenReturn(parentInfo);
        when(parentInfo.getID()).thenReturn(FOLDER_ID);
        when(fileInfo.getCreatedAt()).thenReturn(createdAt);
        when(fileInfo.getModifiedAt()).thenReturn(modifiedAt);
        when(fileInfo.getSha1()).thenReturn("sha1-hash");

        try (MockedConstruction<BoxFile> mockedFile = mockConstruction(
            BoxFile.class,
            (mock, context) -> when(mock.getInfo()).thenReturn(fileInfo)
        )) {
            FileInfoResponse response = fileService.getFileInfo(API_KEY, FILE_ID);

            assertThat(response.getFileId()).isEqualTo(FILE_ID);
            assertThat(response.getFileName()).isEqualTo("sample.txt");
            assertThat(response.getSize()).isEqualTo(1024L);
            assertThat(response.getParentFolderId()).isEqualTo(FOLDER_ID);
            assertThat(response.getSha1()).isEqualTo("sha1-hash");
            assertThat(response.getCreatedAt()).isNotNull();
            assertThat(response.getModifiedAt()).isNotNull();

            verify(rateLimiterManager).handleSuccess(API_KEY);
            assertThat(mockedFile.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("getFileInfo - レートリミットでブロックされた場合に例外となること")
    void getFileInfo_RateLimitGateRejected() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(false);

        assertThatThrownBy(() -> fileService.getFileInfo(API_KEY, FILE_ID))
            .isInstanceOf(BoxApiException.class)
            .hasMessageContaining("レート制限");

        verify(clientManager, never()).getConnection(API_KEY);
    }

    @Test
    @DisplayName("getFileInfo - Box APIが404を返した場合にResourceNotFoundExceptionを送出すること")
    void getFileInfo_NotFound() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException notFound = mock(BoxAPIException.class);
        when(notFound.getResponseCode()).thenReturn(404);

        try (MockedConstruction<BoxFile> mockedFile = mockConstruction(
            BoxFile.class,
            (mock, context) -> when(mock.getInfo()).thenThrow(notFound)
        )) {
            assertThatThrownBy(() -> fileService.getFileInfo(API_KEY, FILE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(FILE_ID);

            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
            assertThat(mockedFile.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("downloadFile - ファイル内容を取得できること")
    void downloadFile_Success() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        byte[] expected = "hello-box".getBytes();

        try (MockedConstruction<BoxFile> mockedFile = mockConstruction(
            BoxFile.class,
            (mock, context) -> stubDownload(mock, expected)
        )) {
            byte[] actual = fileService.downloadFile(API_KEY, FILE_ID);

            assertThat(actual).isEqualTo(expected);
            verify(rateLimiterManager).handleSuccess(API_KEY);
            assertThat(mockedFile.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("downloadFile - Box APIが404を返した場合にResourceNotFoundExceptionを送出すること")
    void downloadFile_NotFound() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException notFound = mock(BoxAPIException.class);
        when(notFound.getResponseCode()).thenReturn(404);

        try (MockedConstruction<BoxFile> mockedFile = mockConstruction(
            BoxFile.class,
            (mock, context) -> doThrow(notFound).when(mock).download(any(OutputStream.class))
        )) {
            assertThatThrownBy(() -> fileService.downloadFile(API_KEY, FILE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(FILE_ID);

            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }

    @Test
    @DisplayName("downloadFile - Box APIが429を返した場合にレートリミットハンドラが呼ばれること")
    void downloadFile_429Handled() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException rateLimit = mock(BoxAPIException.class);
        when(rateLimit.getResponseCode()).thenReturn(429);

        try (MockedConstruction<BoxFile> mockedFile = mockConstruction(
            BoxFile.class,
            (mock, context) -> doThrow(rateLimit).when(mock).download(any(OutputStream.class))
        )) {
            assertThatThrownBy(() -> fileService.downloadFile(API_KEY, FILE_ID))
                .isInstanceOf(BoxApiException.class)
                .hasMessageContaining("ファイルダウンロードに失敗しました");

            verify(rateLimiterManager).handleRateLimitExceeded(API_KEY);
            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }

    @Test
    @DisplayName("deleteFile - ファイルを削除できること")
    void deleteFile_Success() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        try (MockedConstruction<BoxFile> mockedFile = mockConstruction(BoxFile.class)) {
            fileService.deleteFile(API_KEY, FILE_ID);

            verify(rateLimiterManager).handleSuccess(API_KEY);
            assertThat(mockedFile.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("deleteFile - Box APIが404を返した場合にResourceNotFoundExceptionを送出すること")
    void deleteFile_NotFound() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException notFound = mock(BoxAPIException.class);
        when(notFound.getResponseCode()).thenReturn(404);

        try (MockedConstruction<BoxFile> mockedFile = mockConstruction(
            BoxFile.class,
            (mock, context) -> doThrow(notFound).when(mock).delete()
        )) {
            assertThatThrownBy(() -> fileService.deleteFile(API_KEY, FILE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(FILE_ID);

            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }

    @Test
    @DisplayName("deleteFile - Box APIが429を返した場合にレートリミットハンドラが呼ばれること")
    void deleteFile_429Handled() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException rateLimit = mock(BoxAPIException.class);
        when(rateLimit.getResponseCode()).thenReturn(429);

        try (MockedConstruction<BoxFile> mockedFile = mockConstruction(
            BoxFile.class,
            (mock, context) -> doThrow(rateLimit).when(mock).delete()
        )) {
            assertThatThrownBy(() -> fileService.deleteFile(API_KEY, FILE_ID))
                .isInstanceOf(BoxApiException.class)
                .hasMessageContaining("ファイル削除に失敗しました");

            verify(rateLimiterManager).handleRateLimitExceeded(API_KEY);
            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }

    private void stubDownload(BoxFile mockFile, byte[] content) {
        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(0);
            outputStream.write(content);
            return null;
        }).when(mockFile).download(any(OutputStream.class));
    }
}
