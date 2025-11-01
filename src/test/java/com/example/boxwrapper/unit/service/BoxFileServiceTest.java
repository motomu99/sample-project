package com.example.boxwrapper.unit.service;

import com.box.sdk.*;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BoxFileService単体テスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BoxFileService - Unit Tests")
class BoxFileServiceTest {

    @Mock
    private BoxClientManager clientManager;

    @Mock
    private RateLimiterManager rateLimiterManager;

    @Mock
    private BoxAPIConnection mockConnection;

    @Mock
    private BoxFolder mockFolder;

    @Mock
    private BoxFile mockFile;

    @InjectMocks
    private BoxFileService fileService;

    private static final String API_KEY = "test-api-key";
    private static final String FOLDER_ID = "123456";
    private static final String FILE_ID = "789012";

    @BeforeEach
    void setUp() {
        when(clientManager.getConnection(API_KEY)).thenReturn(mockConnection);
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);
    }

    @Test
    @DisplayName("ファイルアップロード - 正常系")
    void uploadFile_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );

        BoxFile.Info mockFileInfo = mock(BoxFile.Info.class);
        when(mockFileInfo.getID()).thenReturn(FILE_ID);
        when(mockFileInfo.getName()).thenReturn("test.txt");
        when(mockFileInfo.getSize()).thenReturn(12L);
        when(mockFileInfo.getCreatedAt()).thenReturn(new Date());

        when(mockConnection.getFolder(FOLDER_ID)).thenReturn(mockFolder);
        when(mockFolder.uploadFile(any(), eq("test.txt"))).thenReturn(mockFileInfo);

        // When
        FileUploadResponse response = fileService.uploadFile(API_KEY, FOLDER_ID, file);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFileId()).isEqualTo(FILE_ID);
        assertThat(response.getFileName()).isEqualTo("test.txt");
        assertThat(response.getSize()).isEqualTo(12L);

        verify(rateLimiterManager).tryConsume(API_KEY);
        verify(rateLimiterManager).handleSuccess(API_KEY);
        verify(mockFolder).uploadFile(any(), eq("test.txt"));
    }

    @Test
    @DisplayName("ファイルアップロード - レート制限超過")
    void uploadFile_RateLimitExceeded() {
        // Given
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(false);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> fileService.uploadFile(API_KEY, FOLDER_ID, file))
            .isInstanceOf(BoxApiException.class)
            .hasMessageContaining("レート制限");
    }

    @Test
    @DisplayName("ファイル情報取得 - 正常系")
    void getFileInfo_Success() {
        // Given
        BoxFile.Info mockFileInfo = mock(BoxFile.Info.class);
        when(mockFileInfo.getID()).thenReturn(FILE_ID);
        when(mockFileInfo.getName()).thenReturn("test.txt");
        when(mockFileInfo.getSize()).thenReturn(12L);
        when(mockFileInfo.getCreatedAt()).thenReturn(new Date());
        when(mockFileInfo.getModifiedAt()).thenReturn(new Date());
        when(mockFileInfo.getSha1()).thenReturn("abc123");

        BoxFolder.Info mockParent = mock(BoxFolder.Info.class);
        when(mockParent.getID()).thenReturn(FOLDER_ID);
        when(mockFileInfo.getParent()).thenReturn(mockParent);

        when(mockConnection.getFile(FILE_ID)).thenReturn(mockFile);
        when(mockFile.getInfo()).thenReturn(mockFileInfo);

        // When
        FileInfoResponse response = fileService.getFileInfo(API_KEY, FILE_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFileId()).isEqualTo(FILE_ID);
        assertThat(response.getFileName()).isEqualTo("test.txt");
        assertThat(response.getSize()).isEqualTo(12L);
        assertThat(response.getParentFolderId()).isEqualTo(FOLDER_ID);
        assertThat(response.getSha1()).isEqualTo("abc123");

        verify(rateLimiterManager).handleSuccess(API_KEY);
    }

    @Test
    @DisplayName("ファイル情報取得 - ファイルが存在しない")
    void getFileInfo_NotFound() {
        // Given
        when(mockConnection.getFile(FILE_ID)).thenReturn(mockFile);
        when(mockFile.getInfo()).thenThrow(new BoxAPIException("Not found", 404, null));

        // When & Then
        assertThatThrownBy(() -> fileService.getFileInfo(API_KEY, FILE_ID))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("File");
    }

    @Test
    @DisplayName("ファイル削除 - 正常系")
    void deleteFile_Success() {
        // Given
        when(mockConnection.getFile(FILE_ID)).thenReturn(mockFile);
        doNothing().when(mockFile).delete();

        // When
        fileService.deleteFile(API_KEY, FILE_ID);

        // Then
        verify(mockFile).delete();
        verify(rateLimiterManager).handleSuccess(API_KEY);
    }
}
