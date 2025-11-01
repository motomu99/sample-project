package com.example.boxwrapper.unit.controller;

import com.example.boxwrapper.controller.FolderController;
import com.example.boxwrapper.exception.ResourceNotFoundException;
import com.example.boxwrapper.model.request.FolderCreateRequest;
import com.example.boxwrapper.model.response.FolderInfoResponse;
import com.example.boxwrapper.service.BoxFolderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FolderControllerのユニットテスト.
 */
@WebMvcTest(FolderController.class)
@DisplayName("FolderController Unit Tests")
class FolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BoxFolderService folderService;

    private static final String API_KEY = "test-api-key-123";
    private static final String TEST_FOLDER_ID = "123456";
    private static final String TEST_PARENT_FOLDER_ID = "0";
    private static final String TEST_FOLDER_NAME = "Test Folder";

    private FolderInfoResponse mockFolderInfo;

    @BeforeEach
    void setUp() {
        mockFolderInfo = FolderInfoResponse.builder()
            .folderId(TEST_FOLDER_ID)
            .folderName(TEST_FOLDER_NAME)
            .parentFolderId(TEST_PARENT_FOLDER_ID)
            .itemCount(5)
            .createdAt(LocalDateTime.now())
            .modifiedAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("POST /api/v1/folders - フォルダ作成が成功すること")
    void testCreateFolder_Success() throws Exception {
        // Given
        FolderCreateRequest request = new FolderCreateRequest();
        request.setParentFolderId(TEST_PARENT_FOLDER_ID);
        request.setFolderName(TEST_FOLDER_NAME);

        when(folderService.createFolder(anyString(), anyString(), anyString()))
            .thenReturn(mockFolderInfo);

        // When & Then
        mockMvc.perform(post("/api/v1/folders")
                .header("X-API-Key", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.folderId").value(TEST_FOLDER_ID))
            .andExpect(jsonPath("$.folderName").value(TEST_FOLDER_NAME))
            .andExpect(jsonPath("$.parentFolderId").value(TEST_PARENT_FOLDER_ID))
            .andExpect(jsonPath("$.itemCount").value(5));

        verify(folderService, times(1))
            .createFolder(API_KEY, TEST_PARENT_FOLDER_ID, TEST_FOLDER_NAME);
    }

    @Test
    @DisplayName("POST /api/v1/folders - APIキーがない場合、401エラーが返ること")
    void testCreateFolder_MissingApiKey() throws Exception {
        // Given
        FolderCreateRequest request = new FolderCreateRequest();
        request.setParentFolderId(TEST_PARENT_FOLDER_ID);
        request.setFolderName(TEST_FOLDER_NAME);

        // When & Then
        mockMvc.perform(post("/api/v1/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());

        verify(folderService, never()).createFolder(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("POST /api/v1/folders - バリデーションエラーの場合、400エラーが返ること")
    void testCreateFolder_ValidationError() throws Exception {
        // Given - folderName is missing
        FolderCreateRequest request = new FolderCreateRequest();
        request.setParentFolderId(TEST_PARENT_FOLDER_ID);
        // folderName not set

        // When & Then
        mockMvc.perform(post("/api/v1/folders")
                .header("X-API-Key", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(folderService, never()).createFolder(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("GET /api/v1/folders/{folderId} - フォルダ情報取得が成功すること")
    void testGetFolderInfo_Success() throws Exception {
        // Given
        when(folderService.getFolderInfo(anyString(), anyString()))
            .thenReturn(mockFolderInfo);

        // When & Then
        mockMvc.perform(get("/api/v1/folders/{folderId}", TEST_FOLDER_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.folderId").value(TEST_FOLDER_ID))
            .andExpect(jsonPath("$.folderName").value(TEST_FOLDER_NAME))
            .andExpect(jsonPath("$.itemCount").value(5));

        verify(folderService, times(1)).getFolderInfo(API_KEY, TEST_FOLDER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/folders/{folderId} - フォルダが存在しない場合、404エラーが返ること")
    void testGetFolderInfo_NotFound() throws Exception {
        // Given
        when(folderService.getFolderInfo(anyString(), anyString()))
            .thenThrow(new ResourceNotFoundException("Folder", TEST_FOLDER_ID));

        // When & Then
        mockMvc.perform(get("/api/v1/folders/{folderId}", TEST_FOLDER_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isNotFound());

        verify(folderService, times(1)).getFolderInfo(API_KEY, TEST_FOLDER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/folders/{folderId}/items - フォルダ内アイテム一覧取得が成功すること")
    void testListFolderItems_Success() throws Exception {
        // Given
        List<String> items = Arrays.asList("file1.txt", "file2.pdf", "subfolder");
        when(folderService.listFolderItems(anyString(), anyString()))
            .thenReturn(items);

        // When & Then
        mockMvc.perform(get("/api/v1/folders/{folderId}/items", TEST_FOLDER_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0]").value("file1.txt"))
            .andExpect(jsonPath("$[1]").value("file2.pdf"))
            .andExpect(jsonPath("$[2]").value("subfolder"));

        verify(folderService, times(1)).listFolderItems(API_KEY, TEST_FOLDER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/folders/{folderId}/items - 空のフォルダの場合、空配列が返ること")
    void testListFolderItems_EmptyFolder() throws Exception {
        // Given
        when(folderService.listFolderItems(anyString(), anyString()))
            .thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/v1/folders/{folderId}/items", TEST_FOLDER_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));

        verify(folderService, times(1)).listFolderItems(API_KEY, TEST_FOLDER_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/folders/{folderId} - フォルダ削除が成功すること（非再帰的）")
    void testDeleteFolder_NonRecursive_Success() throws Exception {
        // Given
        doNothing().when(folderService).deleteFolder(anyString(), anyString(), anyBoolean());

        // When & Then
        mockMvc.perform(delete("/api/v1/folders/{folderId}", TEST_FOLDER_ID)
                .header("X-API-Key", API_KEY)
                .param("recursive", "false"))
            .andExpect(status().isNoContent());

        verify(folderService, times(1)).deleteFolder(API_KEY, TEST_FOLDER_ID, false);
    }

    @Test
    @DisplayName("DELETE /api/v1/folders/{folderId} - フォルダ削除が成功すること（再帰的）")
    void testDeleteFolder_Recursive_Success() throws Exception {
        // Given
        doNothing().when(folderService).deleteFolder(anyString(), anyString(), anyBoolean());

        // When & Then
        mockMvc.perform(delete("/api/v1/folders/{folderId}", TEST_FOLDER_ID)
                .header("X-API-Key", API_KEY)
                .param("recursive", "true"))
            .andExpect(status().isNoContent());

        verify(folderService, times(1)).deleteFolder(API_KEY, TEST_FOLDER_ID, true);
    }

    @Test
    @DisplayName("DELETE /api/v1/folders/{folderId} - recursiveパラメータのデフォルト値がfalseであること")
    void testDeleteFolder_DefaultRecursiveFalse() throws Exception {
        // Given
        doNothing().when(folderService).deleteFolder(anyString(), anyString(), anyBoolean());

        // When & Then
        mockMvc.perform(delete("/api/v1/folders/{folderId}", TEST_FOLDER_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isNoContent());

        verify(folderService, times(1)).deleteFolder(API_KEY, TEST_FOLDER_ID, false);
    }

    @Test
    @DisplayName("DELETE /api/v1/folders/{folderId} - フォルダが存在しない場合、404エラーが返ること")
    void testDeleteFolder_NotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Folder", TEST_FOLDER_ID))
            .when(folderService).deleteFolder(anyString(), anyString(), anyBoolean());

        // When & Then
        mockMvc.perform(delete("/api/v1/folders/{folderId}", TEST_FOLDER_ID)
                .header("X-API-Key", API_KEY))
            .andExpect(status().isNotFound());

        verify(folderService, times(1)).deleteFolder(API_KEY, TEST_FOLDER_ID, false);
    }

    @Test
    @DisplayName("全エンドポイント - APIキー検証が正しく行われること")
    void testApiKeyValidation_AllEndpoints() throws Exception {
        // Test POST /api/v1/folders without API key
        FolderCreateRequest createRequest = new FolderCreateRequest();
        createRequest.setParentFolderId(TEST_PARENT_FOLDER_ID);
        createRequest.setFolderName(TEST_FOLDER_NAME);

        mockMvc.perform(post("/api/v1/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isUnauthorized());

        // Test GET /api/v1/folders/{folderId} without API key
        mockMvc.perform(get("/api/v1/folders/{folderId}", TEST_FOLDER_ID))
            .andExpect(status().isUnauthorized());

        // Test GET /api/v1/folders/{folderId}/items without API key
        mockMvc.perform(get("/api/v1/folders/{folderId}/items", TEST_FOLDER_ID))
            .andExpect(status().isUnauthorized());

        // Test DELETE /api/v1/folders/{folderId} without API key
        mockMvc.perform(delete("/api/v1/folders/{folderId}", TEST_FOLDER_ID))
            .andExpect(status().isUnauthorized());

        // Verify service was never called
        verify(folderService, never()).createFolder(anyString(), anyString(), anyString());
        verify(folderService, never()).getFolderInfo(anyString(), anyString());
        verify(folderService, never()).listFolderItems(anyString(), anyString());
        verify(folderService, never()).deleteFolder(anyString(), anyString(), anyBoolean());
    }
}
