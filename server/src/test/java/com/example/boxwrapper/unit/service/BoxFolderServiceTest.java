package com.example.boxwrapper.unit.service;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxAPIException;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;
import com.example.boxwrapper.client.BoxClientManager;
import com.example.boxwrapper.exception.BoxApiException;
import com.example.boxwrapper.exception.ResourceNotFoundException;
import com.example.boxwrapper.model.response.FolderInfoResponse;
import com.example.boxwrapper.service.BoxFolderService;
import com.example.boxwrapper.utils.RateLimiterManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoxFolderService Unit Tests")
class BoxFolderServiceTest {

    private static final String API_KEY = "test-api-key";
    private static final String FOLDER_ID = "123456";
    private static final String PARENT_ID = "0";
    private static final String FOLDER_NAME = "Test Folder";

    @Mock
    private BoxClientManager clientManager;

    @Mock
    private RateLimiterManager rateLimiterManager;

    @Mock
    private BoxAPIConnection apiConnection;

    private BoxFolderService folderService;

    @BeforeEach
    void setUp() {
        folderService = new BoxFolderService(clientManager, rateLimiterManager);
        lenient().when(clientManager.getConnection(API_KEY)).thenReturn(apiConnection);
    }

    @Test
    @DisplayName("createFolder - 正常にフォルダが作成されること")
    void createFolder_Success() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxFolder.Info createdInfo = mock(BoxFolder.Info.class);
        BoxFolder.Info parentInfo = mock(BoxFolder.Info.class);
        when(createdInfo.getID()).thenReturn(FOLDER_ID);
        when(createdInfo.getName()).thenReturn(FOLDER_NAME);
        when(createdInfo.getParent()).thenReturn(parentInfo);
        when(parentInfo.getID()).thenReturn(PARENT_ID);
        when(createdInfo.getCreatedAt()).thenReturn(java.util.Date.from(Instant.parse("2024-01-01T00:00:00Z")));
        when(createdInfo.getModifiedAt()).thenReturn(java.util.Date.from(Instant.parse("2024-01-02T00:00:00Z")));

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> when(mock.createFolder(FOLDER_NAME)).thenReturn(createdInfo)
        )) {
            FolderInfoResponse response = folderService.createFolder(API_KEY, PARENT_ID, FOLDER_NAME);

            assertThat(response.getFolderId()).isEqualTo(FOLDER_ID);
            assertThat(response.getFolderName()).isEqualTo(FOLDER_NAME);
            assertThat(response.getParentFolderId()).isEqualTo(PARENT_ID);
            assertThat(response.getCreatedAt()).isNotNull();
            assertThat(response.getModifiedAt()).isNotNull();

            verify(rateLimiterManager).handleSuccess(API_KEY);
            assertThat(mockedFolder.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("createFolder - レート制限でブロックされた場合に例外となること")
    void createFolder_RateLimitGateRejected() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(false);

        assertThatThrownBy(() -> folderService.createFolder(API_KEY, PARENT_ID, FOLDER_NAME))
            .isInstanceOf(BoxApiException.class)
            .hasMessageContaining("レート制限");

        verify(clientManager, never()).getConnection(API_KEY);
    }

    @Test
    @DisplayName("createFolder - 親フォルダが存在しない場合にResourceNotFoundExceptionを送出すること")
    void createFolder_ParentNotFound() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException notFound = mock(BoxAPIException.class);
        when(notFound.getResponseCode()).thenReturn(404);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> when(mock.createFolder(FOLDER_NAME)).thenThrow(notFound)
        )) {
            assertThatThrownBy(() -> folderService.createFolder(API_KEY, PARENT_ID, FOLDER_NAME))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(PARENT_ID);

            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
            assertThat(mockedFolder.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("createFolder - Box APIが429を返した場合にレートリミットハンドラが呼ばれること")
    void createFolder_429Handled() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException rateLimit = mock(BoxAPIException.class);
        when(rateLimit.getResponseCode()).thenReturn(429);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> when(mock.createFolder(FOLDER_NAME)).thenThrow(rateLimit)
        )) {
            assertThatThrownBy(() -> folderService.createFolder(API_KEY, PARENT_ID, FOLDER_NAME))
                .isInstanceOf(BoxApiException.class)
                .hasMessageContaining("フォルダ作成に失敗しました");

            verify(rateLimiterManager).handleRateLimitExceeded(API_KEY);
            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }

    @Test
    @DisplayName("getFolderInfo - フォルダ情報を取得できること")
    void getFolderInfo_Success() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxFolder.Info info = mock(BoxFolder.Info.class);
        BoxFolder.Info parent = mock(BoxFolder.Info.class);
        when(info.getID()).thenReturn(FOLDER_ID);
        when(info.getName()).thenReturn(FOLDER_NAME);
        when(info.getParent()).thenReturn(parent);
        when(parent.getID()).thenReturn(PARENT_ID);
        when(info.getCreatedAt()).thenReturn(java.util.Date.from(Instant.parse("2024-03-01T00:00:00Z")));
        when(info.getModifiedAt()).thenReturn(java.util.Date.from(Instant.parse("2024-03-02T00:00:00Z")));

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> when(mock.getInfo()).thenReturn(info)
        )) {
            FolderInfoResponse response = folderService.getFolderInfo(API_KEY, FOLDER_ID);

            assertThat(response.getFolderId()).isEqualTo(FOLDER_ID);
            assertThat(response.getFolderName()).isEqualTo(FOLDER_NAME);
            assertThat(response.getParentFolderId()).isEqualTo(PARENT_ID);

            verify(rateLimiterManager).handleSuccess(API_KEY);
            assertThat(mockedFolder.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("getFolderInfo - レート制限でブロックされた場合に例外となること")
    void getFolderInfo_RateLimitGateRejected() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(false);

        assertThatThrownBy(() -> folderService.getFolderInfo(API_KEY, FOLDER_ID))
            .isInstanceOf(BoxApiException.class)
            .hasMessageContaining("レート制限");

        verify(clientManager, never()).getConnection(API_KEY);
    }

    @Test
    @DisplayName("getFolderInfo - フォルダが存在しない場合にResourceNotFoundExceptionを送出すること")
    void getFolderInfo_NotFound() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException notFound = mock(BoxAPIException.class);
        when(notFound.getResponseCode()).thenReturn(404);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> when(mock.getInfo()).thenThrow(notFound)
        )) {
            assertThatThrownBy(() -> folderService.getFolderInfo(API_KEY, FOLDER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(FOLDER_ID);

            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }

    @Test
    @DisplayName("getFolderInfo - Box APIが429を返した場合にレートリミットハンドラが呼ばれること")
    void getFolderInfo_429Handled() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException rateLimit = mock(BoxAPIException.class);
        when(rateLimit.getResponseCode()).thenReturn(429);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> when(mock.getInfo()).thenThrow(rateLimit)
        )) {
            assertThatThrownBy(() -> folderService.getFolderInfo(API_KEY, FOLDER_ID))
                .isInstanceOf(BoxApiException.class)
                .hasMessageContaining("フォルダ情報取得に失敗しました");

            verify(rateLimiterManager).handleRateLimitExceeded(API_KEY);
            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }

    @Test
    @DisplayName("listFolderItems - フォルダ内アイテムを取得できること")
    void listFolderItems_Success() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxItem.Info item1 = mock(BoxItem.Info.class);
        BoxItem.Info item2 = mock(BoxItem.Info.class);
        when(item1.getName()).thenReturn("file-1");
        when(item2.getName()).thenReturn("folder-2");
        List<BoxItem.Info> items = List.of(item1, item2);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> when(mock.iterator()).thenReturn(items.iterator())
        )) {
            List<String> result = folderService.listFolderItems(API_KEY, FOLDER_ID);

            assertThat(result).containsExactly("file-1", "folder-2");
            verify(rateLimiterManager).handleSuccess(API_KEY);
            assertThat(mockedFolder.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("listFolderItems - レート制限でブロックされた場合に例外となること")
    void listFolderItems_RateLimitGateRejected() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(false);

        assertThatThrownBy(() -> folderService.listFolderItems(API_KEY, FOLDER_ID))
            .isInstanceOf(BoxApiException.class)
            .hasMessageContaining("レート制限");

        verify(clientManager, never()).getConnection(API_KEY);
    }

    @Test
    @DisplayName("listFolderItems - フォルダが存在しない場合にResourceNotFoundExceptionを送出すること")
    void listFolderItems_NotFound() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException notFound = mock(BoxAPIException.class);
        when(notFound.getResponseCode()).thenReturn(404);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> when(mock.iterator()).thenThrow(notFound)
        )) {
            assertThatThrownBy(() -> folderService.listFolderItems(API_KEY, FOLDER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(FOLDER_ID);

            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }

    @Test
    @DisplayName("listFolderItems - Box APIが429を返した場合にレートリミットハンドラが呼ばれること")
    void listFolderItems_429Handled() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException rateLimit = mock(BoxAPIException.class);
        when(rateLimit.getResponseCode()).thenReturn(429);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> when(mock.iterator()).thenThrow(rateLimit)
        )) {
            assertThatThrownBy(() -> folderService.listFolderItems(API_KEY, FOLDER_ID))
                .isInstanceOf(BoxApiException.class)
                .hasMessageContaining("フォルダアイテム取得に失敗しました");

            verify(rateLimiterManager).handleRateLimitExceeded(API_KEY);
            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }

    @Test
    @DisplayName("deleteFolder - 正常に削除できること")
    void deleteFolder_Success() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(BoxFolder.class)) {
            folderService.deleteFolder(API_KEY, FOLDER_ID, true);

            verify(rateLimiterManager).handleSuccess(API_KEY);
            assertThat(mockedFolder.constructed()).hasSize(1);
        }
    }

    @Test
    @DisplayName("deleteFolder - レート制限でブロックされた場合に例外となること")
    void deleteFolder_RateLimitGateRejected() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(false);

        assertThatThrownBy(() -> folderService.deleteFolder(API_KEY, FOLDER_ID, true))
            .isInstanceOf(BoxApiException.class)
            .hasMessageContaining("レート制限");

        verify(clientManager, never()).getConnection(API_KEY);
    }

    @Test
    @DisplayName("deleteFolder - フォルダが存在しない場合にResourceNotFoundExceptionを送出すること")
    void deleteFolder_NotFound() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException notFound = mock(BoxAPIException.class);
        when(notFound.getResponseCode()).thenReturn(404);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> doThrow(notFound).when(mock).delete(true)
        )) {
            assertThatThrownBy(() -> folderService.deleteFolder(API_KEY, FOLDER_ID, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(FOLDER_ID);

            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }

    @Test
    @DisplayName("deleteFolder - Box APIが429を返した場合にレートリミットハンドラが呼ばれること")
    void deleteFolder_429Handled() {
        when(rateLimiterManager.tryConsume(API_KEY)).thenReturn(true);

        BoxAPIException rateLimit = mock(BoxAPIException.class);
        when(rateLimit.getResponseCode()).thenReturn(429);

        try (MockedConstruction<BoxFolder> mockedFolder = mockConstruction(
            BoxFolder.class,
            (mock, context) -> doThrow(rateLimit).when(mock).delete(true)
        )) {
            assertThatThrownBy(() -> folderService.deleteFolder(API_KEY, FOLDER_ID, true))
                .isInstanceOf(BoxApiException.class)
                .hasMessageContaining("フォルダ削除に失敗しました");

            verify(rateLimiterManager).handleRateLimitExceeded(API_KEY);
            verify(rateLimiterManager, never()).handleSuccess(API_KEY);
        }
    }
}
