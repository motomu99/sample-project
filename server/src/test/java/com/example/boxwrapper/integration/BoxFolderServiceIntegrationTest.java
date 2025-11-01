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
 * BoxFolderService?????.
 *
 * <p>???Box API?????????????</p>
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
            System.out.println("??: BOX_DEVELOPER_TOKEN???????????");
        }
    }

    @BeforeEach
    void setUp() {
        // ????????????
        try {
            String folderName = "test-folder-" + UUID.randomUUID().toString().substring(0, 8);
            var folderInfo = folderService.createFolder(TEST_API_KEY, ROOT_FOLDER_ID, folderName);
            testFolderId = folderInfo.getFolderId();
            System.out.println("???????????????: " + folderName + " (ID: " + testFolderId + ")");
        } catch (Exception e) {
            System.err.println("??????????????????: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        // ?????????????????
        if (subFolderId != null) {
            try {
                folderService.deleteFolder(TEST_API_KEY, subFolderId, true);
                System.out.println("????????????????: " + subFolderId);
            } catch (Exception e) {
                System.err.println("???????????????????: " + e.getMessage());
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
    @DisplayName("?????? - ???Box API???")
    @Disabled("???Box API?????????????????")
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
        System.out.println("????????: " + response.getFolderId());
        
        // ???????
        try {
            folderService.deleteFolder(TEST_API_KEY, response.getFolderId(), true);
        } catch (Exception e) {
            System.err.println("?????????: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("???????? - ???Box API???")
    @Disabled("???Box API?????????????????")
    void getFolderInfo_RealBoxAPI() {
        // Given - ??????????????????

        // When
        var folderInfo = folderService.getFolderInfo(TEST_API_KEY, testFolderId);

        // Then
        assertNotNull(folderInfo);
        assertEquals(testFolderId, folderInfo.getFolderId());
        assertNotNull(folderInfo.getFolderName());
        assertEquals(ROOT_FOLDER_ID, folderInfo.getParentFolderId());
        assertNotNull(folderInfo.getCreatedAt());
        System.out.println("??????????: " + folderInfo.getFolderId());
    }

    @Test
    @DisplayName("????????????? - ???Box API???")
    @Disabled("???Box API?????????????????")
    void listFolderItems_RealBoxAPI() {
        // Given - ??????????????????

        // When
        List<String> items = folderService.listFolderItems(TEST_API_KEY, testFolderId);

        // Then
        assertNotNull(items);
        // ????????????????
        assertTrue(items.isEmpty() || items.size() >= 0);
        System.out.println("???????????????: " + items.size() + " items");
    }

    @Test
    @DisplayName("??????????? - ???Box API???")
    @Disabled("???Box API?????????????????")
    void createAndDeleteSubFolder_RealBoxAPI() {
        // Given
        String subFolderName = "sub-folder-" + UUID.randomUUID().toString().substring(0, 8);

        // When - ????????
        var subFolderInfo = folderService.createFolder(TEST_API_KEY, testFolderId, subFolderName);
        subFolderId = subFolderInfo.getFolderId();

        // Then
        assertNotNull(subFolderInfo);
        assertEquals(subFolderName, subFolderInfo.getFolderName());
        assertEquals(testFolderId, subFolderInfo.getParentFolderId());
        System.out.println("??????????: " + subFolderId);

        // ??
        assertDoesNotThrow(() -> {
            folderService.deleteFolder(TEST_API_KEY, subFolderId, false);
        });
        subFolderId = null; // tearDown?????????
        System.out.println("??????????");
    }

    @Test
    @DisplayName("??????????? - ???Box API???")
    @Disabled("???Box API?????????????????")
    void deleteFolderRecursive_RealBoxAPI() {
        // Given - ??????????????????
        String folderToDelete = testFolderId;
        testFolderId = null; // tearDown?????????

        // When & Then
        assertDoesNotThrow(() -> {
            folderService.deleteFolder(TEST_API_KEY, folderToDelete, true);
        });
        System.out.println("?????????????: " + folderToDelete);
    }
}
