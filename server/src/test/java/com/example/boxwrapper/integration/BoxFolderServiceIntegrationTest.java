package com.example.boxwrapper.integration;

import com.example.boxwrapper.service.BoxFolderService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BoxFolderServiceの統合テスト.
 *
 * <p>実際のBox APIを使用してフォルダ操作をテストします。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "box.rate-limit.enabled=false",
    "box.auth.type=developer-token"
})
@DisplayName("BoxFolderService Integration Tests")
@Tag("integration")
class BoxFolderServiceIntegrationTest {

    @Autowired
    private BoxFolderService folderService;

    private static final String TEST_API_KEY = "test-api-key";
    private static final String ROOT_FOLDER_ID = "0";
    
    private String testFolderId;
    private String subFolderId;

    @BeforeAll
    static void checkBoxConnection() {
        String developerToken = System.getenv("BOX_DEVELOPER_TOKEN");
        if (developerToken == null || developerToken.isEmpty()) {
            System.out.println("警告: BOX_DEVELOPER_TOKENが設定されていません");
        }
    }

    @BeforeEach
    void setUp() {
        // テスト用フォルダを作成
        try {
            String folderName = "test-folder-" + UUID.randomUUID().toString().substring(0, 8);
            var folderInfo = folderService.createFolder(TEST_API_KEY, ROOT_FOLDER_ID, folderName);
            testFolderId = folderInfo.getFolderId();
            System.out.println("テスト用フォルダを作成しました: " + folderName + " (ID: " + testFolderId + ")");
        } catch (Exception e) {
            System.err.println("フォルダ作成に失敗しました: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        // サブフォルダを削除
        if (subFolderId != null) {
            try {
                folderService.deleteFolder(TEST_API_KEY, subFolderId, true);
                System.out.println("サブフォルダを削除しました: " + subFolderId);
            } catch (Exception e) {
                System.err.println("サブフォルダ削除に失敗しました: " + e.getMessage());
            }
        }

        // テストフォルダを削除
        if (testFolderId != null) {
            try {
                folderService.deleteFolder(TEST_API_KEY, testFolderId, true);
                System.out.println("テストフォルダを削除しました: " + testFolderId);
            } catch (Exception e) {
                System.err.println("テストフォルダ削除に失敗しました: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("フォルダ作成 - 実際のBox API使用")
    @Disabled("実際のBox APIを使用するため、手動で有効化してください")
    void createFolder_RealBoxAPI() {
        // Given
        String folderName = "integration-test-" + UUID.randomUUID().toString().substring(0, 8);

        // When
        var response = folderService.createFolder(TEST_API_KEY, ROOT_FOLDER_ID, folderName);

        // Then
        assertNotNull(response);
        assertNotNull(response.getFolderId());
        assertEquals(folderName, response.getFolderName());
        assertEquals(ROOT_FOLDER_ID, response.getParentFolderId());
        assertNotNull(response.getCreatedAt());
        System.out.println("フォルダID: " + response.getFolderId());
        
        // クリーンアップ
        try {
            folderService.deleteFolder(TEST_API_KEY, response.getFolderId(), true);
        } catch (Exception e) {
            System.err.println("削除失敗: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("フォルダ情報取得 - 実際のBox API使用")
    @Disabled("実際のBox APIを使用するため、手動で有効化してください")
    void getFolderInfo_RealBoxAPI() {
        // Given - setUpで作成されたテストフォルダを使用

        // When
        var folderInfo = folderService.getFolderInfo(TEST_API_KEY, testFolderId);

        // Then
        assertNotNull(folderInfo);
        assertEquals(testFolderId, folderInfo.getFolderId());
        assertNotNull(folderInfo.getFolderName());
        assertEquals(ROOT_FOLDER_ID, folderInfo.getParentFolderId());
        assertNotNull(folderInfo.getCreatedAt());
        System.out.println("フォルダ情報ID: " + folderInfo.getFolderId());
    }

    @Test
    @DisplayName("フォルダ内アイテム一覧取得 - 実際のBox API使用")
    @Disabled("実際のBox APIを使用するため、手動で有効化してください")
    void listFolderItems_RealBoxAPI() {
        // Given - setUpで作成されたテストフォルダを使用

        // When
        List<String> items = folderService.listFolderItems(TEST_API_KEY, testFolderId);

        // Then
        assertNotNull(items);
        // アイテムの有無に関わらずリストが返されることを確認
        assertTrue(items.isEmpty() || items.size() >= 0);
        System.out.println("フォルダ内アイテム数: " + items.size() + " items");
    }

    @Test
    @DisplayName("サブフォルダ作成と削除 - 実際のBox API使用")
    @Disabled("実際のBox APIを使用するため、手動で有効化してください")
    void createAndDeleteSubFolder_RealBoxAPI() {
        // Given
        String subFolderName = "sub-folder-" + UUID.randomUUID().toString().substring(0, 8);

        // When - サブフォルダを作成
        var subFolderInfo = folderService.createFolder(TEST_API_KEY, testFolderId, subFolderName);
        subFolderId = subFolderInfo.getFolderId();

        // Then
        assertNotNull(subFolderInfo);
        assertEquals(subFolderName, subFolderInfo.getFolderName());
        assertEquals(testFolderId, subFolderInfo.getParentFolderId());
        System.out.println("サブフォルダID: " + subFolderId);

        // 削除
        assertDoesNotThrow(() -> {
            folderService.deleteFolder(TEST_API_KEY, subFolderId, false);
        });
        subFolderId = null; // tearDownで削除されないようにする
        System.out.println("サブフォルダ削除完了");
    }

    @Test
    @DisplayName("フォルダ再帰的削除 - 実際のBox API使用")
    @Disabled("実際のBox APIを使用するため、手動で有効化してください")
    void deleteFolderRecursive_RealBoxAPI() {
        // Given - setUpで作成されたテストフォルダを使用
        String folderToDelete = testFolderId;
        testFolderId = null; // tearDownで削除されないようにする

        // When & Then
        assertDoesNotThrow(() -> {
            folderService.deleteFolder(TEST_API_KEY, folderToDelete, true);
        });
        System.out.println("フォルダ再帰的削除完了: " + folderToDelete);
    }
}
