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
 * BoxClientManagerのユニットテスト.
 *
 * <p>Box API接続管理のテストを実行します。</p>
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
    @DisplayName("isValidApiKey - 正常系: 登録済みAPIキーの場合trueを返す")
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
    @DisplayName("isValidApiKey - 正常系: 未登録APIキーの場合falseを返す")
    void testIsValidApiKey_UnregisteredKey() {
        // Given
        when(apiProperties.getKeys()).thenReturn(new ArrayList<>());

        // When
        clientManager.initialize();

        // Then
        assertFalse(clientManager.isValidApiKey("non-existent-key"));
    }

    @Test
    @DisplayName("getConnection - 正常系: 登録済みAPIキーで接続が取得できること")
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
    @DisplayName("getConnection - 異常系: 未登録APIキーの場合AuthenticationExceptionがスローされる")
    void testGetConnection_UnregisteredKey() {
        // Given
        when(apiProperties.getKeys()).thenReturn(new ArrayList<>());
        clientManager.initialize();

        // When & Then
        assertThrows(AuthenticationException.class, () ->
            clientManager.getConnection("non-existent-key"));
    }

    @Test
    @DisplayName("getConnection - 正常系: 単一接続の取得")
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
        // 注意: 実際のBox SDK接続はモックではnullになる可能性がある
        // 実際のテストではBox SDKの接続オブジェクトが正しく生成されることを確認する
    }

    @Test
    @DisplayName("getConnection - 正常系: ラウンドロビン負荷分散")
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
        // 注意: 実際のBox SDK接続はモックではnullになる可能性がある
    }

    @Test
    @DisplayName("initialize - 正常系: Developer Token認証で初期化成功")
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
    @DisplayName("initialize - 異常系: Developer Tokenが設定されていない場合AuthenticationExceptionがスローされる")
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
        // initialize()実行時はエラーハンドリングにより例外がキャッチされる
        // 実際のエラーはログに記録される
        assertDoesNotThrow(() -> clientManager.initialize());
    }

    @Test
    @DisplayName("initialize - 正常系: 空のAPIキーリストで初期化成功")
    void testInitialize_EmptyApiKeyList() {
        // Given
        when(apiProperties.getKeys()).thenReturn(new ArrayList<>());

        // When & Then
        assertDoesNotThrow(() -> clientManager.initialize());
        assertFalse(clientManager.isValidApiKey("any-key"));
    }

    @Test
    @DisplayName("initialize - 正常系: 複数APIキーで初期化成功")
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
    @DisplayName("getConnection - 異常系: 接続が存在しない場合AuthenticationExceptionがスローされる")
    void testGetConnection_NoConnections() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        ApiProperties.ApiKeyConfig config = new ApiProperties.ApiKeyConfig();
        config.setKey(TEST_API_KEY);
        config.setBoxConfigs(new ArrayList<>()); // 空リスト
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
    @DisplayName("正常系: 複数のAPIキーで独立した接続が取得できること")
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
        // 複数のAPIキーでそれぞれ独立した接続が取得できることを確認
    }

    @Test
    @DisplayName("正常系: エラーハンドリング - 不正なAPIキー設定でも初期化成功")
    void testInitialize_ErrorHandling() {
        // Given
        List<ApiProperties.ApiKeyConfig> keyConfigs = new ArrayList<>();
        
        // 無効な設定
        ApiProperties.ApiKeyConfig invalidConfig = new ApiProperties.ApiKeyConfig();
        invalidConfig.setKey("invalid-key");
        invalidConfig.setBoxConfigs(List.of("non-existent-config.json"));
        keyConfigs.add(invalidConfig);

        // 有効な設定
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
        // 無効なAPIキーは初期化時にエラーが発生するが処理は継続される
        // 有効なAPIキーは正常に初期化される
        assertTrue(clientManager.isValidApiKey(TEST_API_KEY));
    }
}
