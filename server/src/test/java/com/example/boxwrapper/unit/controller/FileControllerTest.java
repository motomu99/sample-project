package com.example.boxwrapper.unit.controller;

import com.example.boxwrapper.controller.FileController;
import com.example.boxwrapper.exception.BoxApiException;
import com.example.boxwrapper.exception.ResourceNotFoundException;
import com.example.boxwrapper.model.response.FileInfoResponse;
import com.example.boxwrapper.model.response.FileUploadResponse;
import com.example.boxwrapper.service.BoxFileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FileControllerのユニットテスト.
 *
 * <p>ファイル操作APIのコントローラーレイヤーのテストを実行します。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileController Unit Tests")
class FileControllerTest {

    @Mock
    private BoxFileService fileService;

    @InjectMocks
    private FileController fileController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String TEST_API_KEY = "test-api-key-123";
    private static final String TEST_FOLDER_ID = "123456";
    private static final String TEST_FILE_ID = "789012";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("uploadFile - 正常系: ファイルアップロード成功")
    void testUploadFile_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "test content".getBytes()
        );

        FileUploadResponse mockResponse = FileUploadResponse.builder()
            .fileId(TEST_FILE_ID)
            .fileName("test.txt")
            .size(12L)
            .createdAt(LocalDateTime.now())
            .build();

        when(fileService.uploadFile(any(), eq(TEST_FOLDER_ID), any(MultipartFile.class)))
            .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/upload")
                .file(file)
                .param("folderId", TEST_FOLDER_ID)
                .requestAttr("apiKey", TEST_API_KEY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fileId").value(TEST_FILE_ID))
            .andExpect(jsonPath("$.fileName").value("test.txt"))
            .andExpect(jsonPath("$.size").value(12L));

        verify(fileService).uploadFile(eq(TEST_API_KEY), eq(TEST_FOLDER_ID), any(MultipartFile.class));
    }

    @Test
    @DisplayName("uploadFile - 異常系: バリデーションエラー")
    void testUploadFile_ValidationError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "test content".getBytes()
        );

        // When & Then - folderIdが空の場合
        mockMvc.perform(multipart("/api/v1/files/upload")
                .file(file)
                .param("folderId", "")
                .requestAttr("apiKey", TEST_API_KEY))
            .andExpect(status().isBadRequest());

        verify(fileService, never()).uploadFile(any(), any(), any());
    }

    @Test
    @DisplayName("uploadFile - 異常系: サービス層でエラーが発生した場合")
    void testUploadFile_ServiceException() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "test content".getBytes()
        );

        when(fileService.uploadFile(any(), eq(TEST_FOLDER_ID), any(MultipartFile.class)))
            .thenThrow(new BoxApiException("Upload failed", 500));

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/upload")
                .file(file)
                .param("folderId", TEST_FOLDER_ID)
                .requestAttr("apiKey", TEST_API_KEY))
            .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("getFileInfo - 正常系: ファイル情報取得成功")
    void testGetFileInfo_Success() {
        // Given
        FileInfoResponse mockResponse = FileInfoResponse.builder()
            .fileId(TEST_FILE_ID)
            .fileName("test.txt")
            .size(12L)
            .parentFolderId(TEST_FOLDER_ID)
            .createdAt(LocalDateTime.now())
            .modifiedAt(LocalDateTime.now())
            .sha1("abc123")
            .build();

        when(fileService.getFileInfo(TEST_API_KEY, TEST_FILE_ID))
            .thenReturn(mockResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("apiKey", TEST_API_KEY);

        // When
        ResponseEntity<FileInfoResponse> response = fileController.getFileInfo(TEST_FILE_ID, request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TEST_FILE_ID, response.getBody().getFileId());
        assertEquals("test.txt", response.getBody().getFileName());
        assertEquals(12L, response.getBody().getSize());

        verify(fileService).getFileInfo(TEST_API_KEY, TEST_FILE_ID);
    }

    @Test
    @DisplayName("getFileInfo - 異常系: ファイルが見つからない場合ResourceNotFoundExceptionがスローされる")
    void testGetFileInfo_NotFound() {
        // Given
        when(fileService.getFileInfo(TEST_API_KEY, TEST_FILE_ID))
            .thenThrow(new ResourceNotFoundException("File", TEST_FILE_ID));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("apiKey", TEST_API_KEY);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
            fileController.getFileInfo(TEST_FILE_ID, request));
    }

    @Test
    @DisplayName("downloadFile - 正常系: ファイルダウンロード成功")
    void testDownloadFile_Success() {
        // Given
        byte[] fileContent = "test file content".getBytes();

        when(fileService.downloadFile(TEST_API_KEY, TEST_FILE_ID))
            .thenReturn(fileContent);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("apiKey", TEST_API_KEY);

        // When
        ResponseEntity<byte[]> response = fileController.downloadFile(TEST_FILE_ID, request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertArrayEquals(fileContent, response.getBody());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());

        verify(fileService).downloadFile(TEST_API_KEY, TEST_FILE_ID);
    }

    @Test
    @DisplayName("downloadFile - 異常系: ファイルが見つからない場合ResourceNotFoundExceptionがスローされる")
    void testDownloadFile_NotFound() {
        // Given
        when(fileService.downloadFile(TEST_API_KEY, TEST_FILE_ID))
            .thenThrow(new ResourceNotFoundException("File", TEST_FILE_ID));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("apiKey", TEST_API_KEY);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
            fileController.downloadFile(TEST_FILE_ID, request));
    }

    @Test
    @DisplayName("downloadFile - 正常系: Content-Dispositionヘッダーが正しく設定される")
    void testDownloadFile_ContentDisposition() {
        // Given
        byte[] fileContent = "test content".getBytes();

        when(fileService.downloadFile(TEST_API_KEY, TEST_FILE_ID))
            .thenReturn(fileContent);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("apiKey", TEST_API_KEY);

        // When
        ResponseEntity<byte[]> response = fileController.downloadFile(TEST_FILE_ID, request);

        // Then
        assertTrue(response.getHeaders().getContentDisposition().isAttachment());
    }

    @Test
    @DisplayName("deleteFile - 正常系: ファイル削除成功")
    void testDeleteFile_Success() {
        // Given
        doNothing().when(fileService).deleteFile(TEST_API_KEY, TEST_FILE_ID);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("apiKey", TEST_API_KEY);

        // When
        ResponseEntity<Void> response = fileController.deleteFile(TEST_FILE_ID, request);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(fileService).deleteFile(TEST_API_KEY, TEST_FILE_ID);
    }

    @Test
    @DisplayName("deleteFile - 異常系: ファイルが見つからない場合ResourceNotFoundExceptionがスローされる")
    void testDeleteFile_NotFound() {
        // Given
        doThrow(new ResourceNotFoundException("File", TEST_FILE_ID))
            .when(fileService).deleteFile(TEST_API_KEY, TEST_FILE_ID);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("apiKey", TEST_API_KEY);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
            fileController.deleteFile(TEST_FILE_ID, request));
    }

    @Test
    @DisplayName("deleteFile - 異常系: サービス層でエラーが発生した場合")
    void testDeleteFile_ServiceException() {
        // Given
        doThrow(new BoxApiException("Delete failed", 500))
            .when(fileService).deleteFile(TEST_API_KEY, TEST_FILE_ID);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("apiKey", TEST_API_KEY);

        // When & Then
        assertThrows(BoxApiException.class, () ->
            fileController.deleteFile(TEST_FILE_ID, request));
    }

    @Test
    @DisplayName("複数エンドポイント - APIキーの抽出が正しく動作すること")
    void testMultipleEndpoints_ApiKeyExtraction() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("apiKey", TEST_API_KEY);

        FileInfoResponse mockInfo = FileInfoResponse.builder()
            .fileId(TEST_FILE_ID)
            .fileName("test.txt")
            .size(12L)
            .build();

        when(fileService.getFileInfo(TEST_API_KEY, TEST_FILE_ID))
            .thenReturn(mockInfo);

        when(fileService.downloadFile(TEST_API_KEY, TEST_FILE_ID))
            .thenReturn("content".getBytes());

        doNothing().when(fileService).deleteFile(TEST_API_KEY, TEST_FILE_ID);

        // When
        fileController.getFileInfo(TEST_FILE_ID, request);
        fileController.downloadFile(TEST_FILE_ID, request);
        fileController.deleteFile(TEST_FILE_ID, request);

        // Then
        verify(fileService).getFileInfo(TEST_API_KEY, TEST_FILE_ID);
        verify(fileService).downloadFile(TEST_API_KEY, TEST_FILE_ID);
        verify(fileService).deleteFile(TEST_API_KEY, TEST_FILE_ID);
    }

    @Test
    @DisplayName("uploadFile - 正常系: 大きなファイルのアップロード")
    void testUploadFile_LargeFile() throws Exception {
        // Given
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "large.txt",
            MediaType.TEXT_PLAIN_VALUE,
            largeContent
        );

        FileUploadResponse mockResponse = FileUploadResponse.builder()
            .fileId(TEST_FILE_ID)
            .fileName("large.txt")
            .size((long) largeContent.length)
            .createdAt(LocalDateTime.now())
            .build();

        when(fileService.uploadFile(any(), eq(TEST_FOLDER_ID), any(MultipartFile.class)))
            .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/files/upload")
                .file(file)
                .param("folderId", TEST_FOLDER_ID)
                .requestAttr("apiKey", TEST_API_KEY))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fileId").value(TEST_FILE_ID))
            .andExpect(jsonPath("$.size").value(largeContent.length));
    }

    @Test
    @DisplayName("getFileInfo - 正常系: 空ファイルの情報取得")
    void testGetFileInfo_EmptyFileInfo() {
        // Given
        FileInfoResponse mockResponse = FileInfoResponse.builder()
            .fileId(TEST_FILE_ID)
            .fileName("empty.txt")
            .size(0L)
            .build();

        when(fileService.getFileInfo(TEST_API_KEY, TEST_FILE_ID))
            .thenReturn(mockResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("apiKey", TEST_API_KEY);

        // When
        ResponseEntity<FileInfoResponse> response = fileController.getFileInfo(TEST_FILE_ID, request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0L, response.getBody().getSize());
    }
}
