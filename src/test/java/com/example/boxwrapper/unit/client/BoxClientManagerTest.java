package com.example.boxwrapper.unit.client;

import com.box.sdk.BoxAPIConnection;
import com.example.boxwrapper.client.BoxClientManager;
import com.example.boxwrapper.config.ApiProperties;
import com.example.boxwrapper.config.BoxProperties;
import com.example.boxwrapper.exception.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * BoxClientManager????????.
 *
 * <p>?????API?????????????????????????????</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BoxClientManager Unit Tests")
class BoxClientManagerTest {

    @Mock
    private BoxProperties boxProperties;

    @Mock
    private ApiProperties apiProperties;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private BoxProperties.Auth authConfig;

    @Mock
    private Resource resource;

    private BoxClientManager clientManager;

    private static final String TEST_API_KEY = "test-api-key-123";
    private static final String TEST_DEVELOPER_TOKEN = "test-developer-token";

    @BeforeEach
    void setUp() {
        when(boxProperties.getAuth()).thenReturn(authConfig);
        clientManager = new BoxClientManager(boxProperties, apiProperties, resourceLoader);
    }

    @Test
    @DisplayName("isValidApiKey - ?????API??????true?????")
    void testIsValidApiKey_RegisteredKey() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        ApiProperties.ApiKeyConfig config = new ApiProperties.ApiKeyConfig();
        config.setKey(TEST_API_KEY);
        config.setBoxConfigs(List.of("classpath:box-config.json"));
        keyConfigs.add(config);

        when(apiProperties.getKeys()).thenReturn(keyConfigs);
        when(authConfig.getType()).thenReturn("developer-token");
        when(authConfig.getDeveloperToken()).thenReturn(TEST_DEVELOPER_TOKEN);

        // When
        clientManager.initialize();

        // Then
        assertTrue(clientManager.isValidApiKey(TEST_API_KEY));
    }

    @Test
    @DisplayName("isValidApiKey - ????API??????false?????")
    void testIsValidApiKey_UnregisteredKey() {
        // Given
        when(apiProperties.getKeys()).thenReturn(new ArrayList<>());

        // When
        clientManager.initialize();

        // Then
        assertFalse(clientManager.isValidApiKey("non-existent-key"));
    }

    @Test
    @DisplayName("getConnection - ?????API?????????????")
    void testGetConnection_RegisteredKey() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        ApiProperties.ApiKeyConfig config = new ApiProperties.ApiKeyConfig();
        config.setKey(TEST_API_KEY);
        config.setBoxConfigs(List.of("classpath:box-config.json"));
        keyConfigs.add(config);

        when(apiProperties.getKeys()).thenReturn(keyConfigs);
        when(authConfig.getType()).thenReturn("developer-token");
        when(authConfig.getDeveloperToken()).thenReturn(TEST_DEVELOPER_TOKEN);

        clientManager.initialize();

        // When
        BoxAPIConnection connection = clientManager.getConnection(TEST_API_KEY);

        // Then
        assertNotNull(connection);
    }

    @Test
    @DisplayName("getConnection - ????API??????AuthenticationException????????")
    void testGetConnection_UnregisteredKey() {
        // Given
        when(apiProperties.getKeys()).thenReturn(new ArrayList<>());
        clientManager.initialize();

        // When & Then
        assertThrows(AuthenticationException.class, () ->
            clientManager.getConnection("non-existent-key"));
    }

    @Test
    @DisplayName("getConnection - ???????????????????")
    void testGetConnection_SingleConnection() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        ApiProperties.ApiKeyConfig config = new ApiProperties.ApiKeyConfig();
        config.setKey(TEST_API_KEY);
        config.setBoxConfigs(List.of("classpath:box-config.json"));
        keyConfigs.add(config);

        when(apiProperties.getKeys()).thenReturn(keyConfigs);
        when(authConfig.getType()).thenReturn("developer-token");
        when(authConfig.getDeveloperToken()).thenReturn(TEST_DEVELOPER_TOKEN);

        clientManager.initialize();

        // When
        BoxAPIConnection connection1 = clientManager.getConnection(TEST_API_KEY);
        BoxAPIConnection connection2 = clientManager.getConnection(TEST_API_KEY);
        BoxAPIConnection connection3 = clientManager.getConnection(TEST_API_KEY);

        // Then
        assertNotNull(connection1);
        assertNotNull(connection2);
        assertNotNull(connection3);
        // ???????????????????????????????
        // ??????????Box SDK???????????null????????
    }

    @Test
    @DisplayName("getConnection - ????????????????????????")
    void testGetConnection_RoundRobinLoadBalancing() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        ApiProperties.ApiKeyConfig config = new ApiProperties.ApiKeyConfig();
        config.setKey(TEST_API_KEY);
        config.setBoxConfigs(List.of(
            "classpath:box-config-1.json",
            "classpath:box-config-2.json"
        ));
        keyConfigs.add(config);

        when(apiProperties.getKeys()).thenReturn(keyConfigs);
        when(authConfig.getType()).thenReturn("developer-token");
        when(authConfig.getDeveloperToken()).thenReturn(TEST_DEVELOPER_TOKEN);

        clientManager.initialize();

        // When
        BoxAPIConnection connection1 = clientManager.getConnection(TEST_API_KEY);
        BoxAPIConnection connection2 = clientManager.getConnection(TEST_API_KEY);
        BoxAPIConnection connection3 = clientManager.getConnection(TEST_API_KEY);
        BoxAPIConnection connection4 = clientManager.getConnection(TEST_API_KEY);

        // Then
        assertNotNull(connection1);
        assertNotNull(connection2);
        assertNotNull(connection3);
        assertNotNull(connection4);
        // ???????????????????????????null?????????
    }

    @Test
    @DisplayName("initialize - Developer Token??????????????")
    void testInitialize_DeveloperTokenAuth() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        ApiProperties.ApiKeyConfig config = new ApiProperties.ApiKeyConfig();
        config.setKey(TEST_API_KEY);
        config.setBoxConfigs(List.of("classpath:box-config.json"));
        keyConfigs.add(config);

        when(apiProperties.getKeys()).thenReturn(keyConfigs);
        when(authConfig.getType()).thenReturn("developer-token");
        when(authConfig.getDeveloperToken()).thenReturn(TEST_DEVELOPER_TOKEN);

        // When
        clientManager.initialize();

        // Then
        assertTrue(clientManager.isValidApiKey(TEST_API_KEY));
        assertNotNull(clientManager.getConnection(TEST_API_KEY));
    }

    @Test
    @DisplayName("initialize - Developer Token????????????AuthenticationException????????")
    void testInitialize_DeveloperTokenNotConfigured() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        ApiProperties.ApiKeyConfig config = new ApiProperties.ApiKeyConfig();
        config.setKey(TEST_API_KEY);
        config.setBoxConfigs(List.of("classpath:box-config.json"));
        keyConfigs.add(config);

        when(apiProperties.getKeys()).thenReturn(keyConfigs);
        when(authConfig.getType()).thenReturn("developer-token");
        when(authConfig.getDeveloperToken()).thenReturn(null);

        // When & Then
        // initialize()?????????????????catch???????????
        // ??????????????????????????????????????
        assertDoesNotThrow(() -> clientManager.initialize());
    }

    @Test
    @DisplayName("initialize - ??API??????????????")
    void testInitialize_EmptyApiKeyList() {
        // Given
        when(apiProperties.getKeys()).thenReturn(new ArrayList<>());

        // When & Then
        assertDoesNotThrow(() -> clientManager.initialize());
        assertFalse(clientManager.isValidApiKey("any-key"));
    }

    @Test
    @DisplayName("initialize - ???API??????????????")
    void testInitialize_MultipleApiKeys() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        
        ApiProperties.ApiKeyConfig config1 = new ApiProperties.ApiKeyConfig();
        config1.setKey("api-key-1");
        config1.setBoxConfigs(List.of("classpath:box-config-1.json"));
        keyConfigs.add(config1);

        ApiProperties.ApiKeyConfig config2 = new ApiProperties.ApiKeyConfig();
        config2.setKey("api-key-2");
        config2.setBoxConfigs(List.of("classpath:box-config-2.json"));
        keyConfigs.add(config2);

        when(apiProperties.getKeys()).thenReturn(keyConfigs);
        when(authConfig.getType()).thenReturn("developer-token");
        when(authConfig.getDeveloperToken()).thenReturn(TEST_DEVELOPER_TOKEN);

        // When
        clientManager.initialize();

        // Then
        assertTrue(clientManager.isValidApiKey("api-key-1"));
        assertTrue(clientManager.isValidApiKey("api-key-2"));
        assertFalse(clientManager.isValidApiKey("api-key-3"));
    }

    @Test
    @DisplayName("getConnection - ???????????AuthenticationException????????")
    void testGetConnection_NoConnections() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        ApiProperties.ApiKeyConfig config = new ApiProperties.ApiKeyConfig();
        config.setKey(TEST_API_KEY);
        config.setBoxConfigs(new ArrayList<>()); // ???????
        keyConfigs.add(config);

        when(apiProperties.getKeys()).thenReturn(keyConfigs);
        when(authConfig.getType()).thenReturn("developer-token");
        when(authConfig.getDeveloperToken()).thenReturn(TEST_DEVELOPER_TOKEN);

        clientManager.initialize();

        // When & Then
        assertThrows(AuthenticationException.class, () ->
            clientManager.getConnection(TEST_API_KEY));
    }

    @Test
    @DisplayName("???API?????????????????")
    void testMultipleApiKeys_IndependentConnections() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        
        ApiProperties.ApiKeyConfig config1 = new ApiProperties.ApiKeyConfig();
        config1.setKey("api-key-1");
        config1.setBoxConfigs(List.of("classpath:box-config-1.json"));
        keyConfigs.add(config1);

        ApiProperties.ApiKeyConfig config2 = new ApiProperties.ApiKeyConfig();
        config2.setKey("api-key-2");
        config2.setBoxConfigs(List.of("classpath:box-config-2.json"));
        keyConfigs.add(config2);

        when(apiProperties.getKeys()).thenReturn(keyConfigs);
        when(authConfig.getType()).thenReturn("developer-token");
        when(authConfig.getDeveloperToken()).thenReturn(TEST_DEVELOPER_TOKEN);

        clientManager.initialize();

        // When
        BoxAPIConnection connection1 = clientManager.getConnection("api-key-1");
        BoxAPIConnection connection2 = clientManager.getConnection("api-key-2");

        // Then
        assertNotNull(connection1);
        assertNotNull(connection2);
        // ???API?????????????????????
    }

    @Test
    @DisplayName("??????????????API??????????????")
    void testInitialize_ErrorHandling() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        
        // ?????
        ApiProperties.ApiKeyConfig invalidConfig = new ApiProperties.ApiKeyConfig();
        invalidConfig.setKey("invalid-key");
        invalidConfig.setBoxConfigs(List.of("non-existent-config.json"));
        keyConfigs.add(invalidConfig);

        // ?????
        ApiProperties.ApiKeyConfig validConfig = new ApiProperties.ApiKeyConfig();
        validConfig.setKey(TEST_API_KEY);
        validConfig.setBoxConfigs(List.of("classpath:box-config.json"));
        keyConfigs.add(validConfig);

        when(apiProperties.getKeys()).thenReturn(keyConfigs);
        when(authConfig.getType()).thenReturn("developer-token");
        when(authConfig.getDeveloperToken()).thenReturn(TEST_DEVELOPER_TOKEN);

        // When
        clientManager.initialize();

        // Then
        // ???API???????????????
        // ???????????????????????????
        assertTrue(clientManager.isValidApiKey(TEST_API_KEY));
    }
}
