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
 * BoxFileService?????.
 *
 * <p>???Box API?????????????
 * ?????????????????????????</p>
 * <ul>
 *   <li>BOX_DEVELOPER_TOKEN: Box Developer Token</li>
 *   <li>????box-config.json?????JWT????</li>
 * </ul>
 *
 * <p>??: ?????????Box API????????
 * ???????Box?????????????????</p>
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
        // Box API????????????
        String developerToken = System.getenv("BOX_DEVELOPER_TOKEN");
        if (developerToken == null || developerToken.isEmpty()) {
            System.out.println("??: BOX_DEVELOPER_TOKEN???????????");
            System.out.println("????????????????Box Developer Token??????");
        }
    }

    @BeforeEach
    void setUp() {
        // ????????????
        try {
            String folderName = "test-integration-" + UUID.randomUUID().toString().substring(0, 8);
            var folderInfo = folderService.createFolder(TEST_API_KEY, ROOT_FOLDER_ID, folderName);
            testFolderId = folderInfo.getFolderId();
            System.out.println("???????????????: " + folderName + " (ID: " + testFolderId + ")");
        } catch (Exception e) {
            System.err.println("??????????????????: " + e.getMessage());
            // ??????????????
        }
    }

    @AfterEach
    void tearDown() {
        // ???????????????
        if (uploadedFileId != null) {
            try {
                fileService.deleteFile(TEST_API_KEY, uploadedFileId);
                System.out.println("??????????????: " + uploadedFileId);
            } catch (Exception e) {
                System.err.println("?????????????????: " + e.getMessage());
            }
        }

        // ???????????
        if (testFolderId != null) {
            try {
                folderService.deleteFolder(TEST_API_KEY, testFolderId, true);
                System.out.println("??????????????: " + testFolderId);
            } catch (Exception e) {
                System.err.println("?????????????????: " + e.getMessage());
            }
        }
    }

    @Test
    @DisplayName("?????????? - ???Box API???")
    @Disabled("???Box API?????????????????")
    void uploadFile_RealBoxAPI() throws IOException {
        // Given
        String fileName = "test-integration-" + UUID.randomUUID() + ".txt";
        String content = "?????????????????\nIntegration test file content.";
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
        System.out.println("????????????: " + response.getFileId());
    }

    @Test
    @DisplayName("???????? - ???Box API???")
    @Disabled("???Box API?????????????????")
    void getFileInfo_RealBoxAPI() throws IOException {
        // Given - ?????????????
        String fileName = "test-info-" + UUID.randomUUID() + ".txt";
        String content = "???????????";
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
        System.out.println("??????????: " + fileInfo.getFileId());
    }

    @Test
    @DisplayName("?????????? - ???Box API???")
    @Disabled("???Box API?????????????????")
    void downloadFile_RealBoxAPI() throws IOException {
        // Given - ?????????????
        String fileName = "test-download-" + UUID.randomUUID() + ".txt";
        String content = "????????????????";
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
        System.out.println("????????????: " + downloadedContent.length + " bytes");
    }

    @Test
    @DisplayName("?????? - ???Box API???")
    @Disabled("???Box API?????????????????")
    void deleteFile_RealBoxAPI() throws IOException {
        // Given - ?????????????
        String fileName = "test-delete-" + UUID.randomUUID() + ".txt";
        String content = "???????????";
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

        // Then - ?????????????????????404???????
        assertThrows(Exception.class, () -> {
            fileService.getFileInfo(TEST_API_KEY, fileIdToDelete);
        });
        
        uploadedFileId = null; // tearDown?????????
        System.out.println("????????: " + fileIdToDelete);
    }

    @Test
    @DisplayName("?????????? ? ???? ? ?????? ? ????????")
    @Disabled("???Box API?????????????????")
    void fileLifecycle_RealBoxAPI() throws IOException {
        // Given
        String fileName = "test-lifecycle-" + UUID.randomUUID() + ".txt";
        String content = "?????????????????????\n" + System.currentTimeMillis();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            fileName,
            "text/plain",
            content.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then - ??????
        var uploadResponse = fileService.uploadFile(TEST_API_KEY, testFolderId, file);
        assertNotNull(uploadResponse.getFileId());
        uploadedFileId = uploadResponse.getFileId();
        System.out.println("1. ????????: " + uploadedFileId);

        // ????
        var fileInfo = fileService.getFileInfo(TEST_API_KEY, uploadedFileId);
        assertEquals(fileName, fileInfo.getFileName());
        System.out.println("2. ??????: " + fileInfo.getFileName());

        // ??????
        byte[] downloadedContent = fileService.downloadFile(TEST_API_KEY, uploadedFileId);
        assertEquals(content, new String(downloadedContent, StandardCharsets.UTF_8));
        System.out.println("3. ????????: " + downloadedContent.length + " bytes");

        // ??
        assertDoesNotThrow(() -> {
            fileService.deleteFile(TEST_API_KEY, uploadedFileId);
        });
        System.out.println("4. ????");
        
        uploadedFileId = null; // tearDown?????????
    }
}
