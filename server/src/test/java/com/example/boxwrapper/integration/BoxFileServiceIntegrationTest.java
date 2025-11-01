package com.example.boxwrapper.integration;

import com.example.boxwrapper.service.BoxFileService;
import com.example.boxwrapper.service.BoxFolderService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BoxFileServiceの統合テスト.
 *
 * <p>実際のBox APIを使用してファイル操作をテストします。
 * 以下の認証方法が使用できます。</p>
 * <ul>
 *   <li>BOX_DEVELOPER_TOKEN: Box Developer Token</li>
 *   <li>またはbox-config.jsonにJWT認証設定を配置</li>
 * </ul>
 *
 * <p>注意: 実際のBox APIを使用するため
 * 実際のBoxアカウントにリソースが作成されます。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "box.rate-limit.enabled=false",
    "box.auth.type=developer-token"
})
@DisplayName("BoxFileService Integration Tests")
@Tag("integration")
class BoxFileServiceIntegrationTest {

    @Autowired
    private BoxFileService fileService;

    @Autowired
    private BoxFolderService folderService;

    private static final String TEST_API_KEY = "test-api-key";
    private static final String ROOT_FOLDER_ID = "0";
    
    private String testFolderId;
    private String uploadedFileId;

    @BeforeAll
    static void checkBoxConnection() {
        // Box API接続の確認
        String developerToken = System.getenv("BOX_DEVELOPER_TOKEN");
        if (developerToken == null || developerToken.isEmpty()) {
            System.out.println("警告: BOX_DEVELOPER_TOKENが設定されていません");
            System.out.println("統合テストには有効なBox Developer Tokenが必要です");
        }
    }

    @BeforeEach
    void setUp() {
        // テスト用フォルダを作成
        try {
            String folderName = "test-integration-" + UUID.randomUUID().toString().substring(0, 8);
            var folderInfo = folderService.createFolder(TEST_API_KEY, ROOT_FOLDER_ID, folderName);
            testFolderId = folderInfo.getFolderId();
            System.out.println("テスト用フォルダを作成しました: " + folderName + " (ID: " + testFolderId + ")");
        } catch (Exception e) {
            System.err.println("フォルダ作成に失敗しました: " + e.getMessage());
            // テスト継続のためエラーは無視
        }
    }

    @AfterEach
    void tearDown() {
        // アップロードしたファイルを削除
        if (uploadedFileId != null) {
            try {
                fileService.deleteFile(TEST_API_KEY, uploadedFileId);
                System.out.println("アップロードファイルを削除しました: " + uploadedFileId);
            } catch (Exception e) {
                System.err.println("ファイル削除に失敗しました: " + e.getMessage());
            }
        }

        // テストフォルダを削除
        if (testFolderId != null) {
            try {
                folderService.deleteFolder(TEST_API_KEY, testFolderId, true);
                System.out.println("テストフォルダを削除しました: " + testFolderId);
            } catch (Exception e) {
                System.err.println("フォルダ削除に失敗しました: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("ファイルアップロード - 実際のBox API使用")
    @Disabled("実際のBox APIを使用するため、手動で有効化してください")
    void uploadFile_RealBoxAPI() throws IOException {
        // Given
        String fileName = "test-integration-" + UUID.randomUUID() + ".txt";
        String content = "これは統合テストファイルです\nIntegration test file content.";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            fileName,
            "text/plain",
            content.getBytes(StandardCharsets.UTF_8)
        );

        // When
        var response = fileService.uploadFile(TEST_API_KEY, testFolderId, file);
        uploadedFileId = response.getFileId();

        // Then
        assertNotNull(response);
        assertNotNull(response.getFileId());
        assertEquals(fileName, response.getFileName());
        assertEquals(content.getBytes(StandardCharsets.UTF_8).length, response.getSize());
        assertNotNull(response.getCreatedAt());
        System.out.println("アップロード成功: " + response.getFileId());
    }

    @Test
    @DisplayName("ファイル情報取得 - 実際のBox API使用")
    @Disabled("実際のBox APIを使用するため、手動で有効化してください")
    void getFileInfo_RealBoxAPI() throws IOException {
        // Given - テストファイルをアップロード
        String fileName = "test-info-" + UUID.randomUUID() + ".txt";
        String content = "テストファイルです";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            fileName,
            "text/plain",
            content.getBytes(StandardCharsets.UTF_8)
        );

        var uploadResponse = fileService.uploadFile(TEST_API_KEY, testFolderId, file);
        uploadedFileId = uploadResponse.getFileId();

        // When
        var fileInfo = fileService.getFileInfo(TEST_API_KEY, uploadedFileId);

        // Then
        assertNotNull(fileInfo);
        assertEquals(uploadedFileId, fileInfo.getFileId());
        assertEquals(fileName, fileInfo.getFileName());
        assertEquals(content.getBytes(StandardCharsets.UTF_8).length, fileInfo.getSize());
        assertEquals(testFolderId, fileInfo.getParentFolderId());
        assertNotNull(fileInfo.getCreatedAt());
        assertNotNull(fileInfo.getModifiedAt());
        System.out.println("ファイル情報取得: " + fileInfo.getFileId());
    }

    @Test
    @DisplayName("ファイルダウンロード - 実際のBox API使用")
    @Disabled("実際のBox APIを使用するため、手動で有効化してください")
    void downloadFile_RealBoxAPI() throws IOException {
        // Given - テストファイルをアップロード
        String fileName = "test-download-" + UUID.randomUUID() + ".txt";
        String content = "ダウンロードテストファイル";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            fileName,
            "text/plain",
            content.getBytes(StandardCharsets.UTF_8)
        );

        var uploadResponse = fileService.uploadFile(TEST_API_KEY, testFolderId, file);
        uploadedFileId = uploadResponse.getFileId();

        // When
        byte[] downloadedContent = fileService.downloadFile(TEST_API_KEY, uploadedFileId);

        // Then
        assertNotNull(downloadedContent);
        assertEquals(content, new String(downloadedContent, StandardCharsets.UTF_8));
        System.out.println("ダウンロード成功: " + downloadedContent.length + " bytes");
    }

    @Test
    @DisplayName("ファイル削除 - 実際のBox API使用")
    @Disabled("実際のBox APIを使用するため、手動で有効化してください")
    void deleteFile_RealBoxAPI() throws IOException {
        // Given - テストファイルをアップロード
        String fileName = "test-delete-" + UUID.randomUUID() + ".txt";
        String content = "削除テストファイル";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            fileName,
            "text/plain",
            content.getBytes(StandardCharsets.UTF_8)
        );

        var uploadResponse = fileService.uploadFile(TEST_API_KEY, testFolderId, file);
        String fileIdToDelete = uploadResponse.getFileId();

        // When
        assertDoesNotThrow(() -> {
            fileService.deleteFile(TEST_API_KEY, fileIdToDelete);
        });

        // Then - 削除後はファイル情報取得で404エラーが発生することを確認
        assertThrows(Exception.class, () -> {
            fileService.getFileInfo(TEST_API_KEY, fileIdToDelete);
        });
        
        uploadedFileId = null; // tearDownで削除されないようにする
        System.out.println("削除完了: " + fileIdToDelete);
    }

    @Test
    @DisplayName("ファイルライフサイクル - アップロード・取得・ダウンロード・削除")
    @Disabled("実際のBox APIを使用するため、手動で有効化してください")
    void fileLifecycle_RealBoxAPI() throws IOException {
        // Given
        String fileName = "test-lifecycle-" + UUID.randomUUID() + ".txt";
        String content = "ライフサイクルテストファイルです\n" + System.currentTimeMillis();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            fileName,
            "text/plain",
            content.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then - アップロード
        var uploadResponse = fileService.uploadFile(TEST_API_KEY, testFolderId, file);
        assertNotNull(uploadResponse.getFileId());
        uploadedFileId = uploadResponse.getFileId();
        System.out.println("1. アップロード成功: " + uploadedFileId);

        // 情報取得
        var fileInfo = fileService.getFileInfo(TEST_API_KEY, uploadedFileId);
        assertEquals(fileName, fileInfo.getFileName());
        System.out.println("2. ファイル名: " + fileInfo.getFileName());

        // ダウンロード
        byte[] downloadedContent = fileService.downloadFile(TEST_API_KEY, uploadedFileId);
        assertEquals(content, new String(downloadedContent, StandardCharsets.UTF_8));
        System.out.println("3. ダウンロード成功: " + downloadedContent.length + " bytes");

        // 削除
        assertDoesNotThrow(() -> {
            fileService.deleteFile(TEST_API_KEY, uploadedFileId);
        });
        System.out.println("4. 削除完了");
        
        uploadedFileId = null; // tearDownで削除されないようにする
    }
}
