package com.example.boxwrapper.client;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxConfig;
import com.box.sdk.BoxDeveloperEditionAPIConnection;
import com.box.sdk.IAccessTokenCache;
import com.box.sdk.InMemoryLRUAccessTokenCache;
import com.example.boxwrapper.config.ApiProperties;
import com.example.boxwrapper.config.BoxProperties;
import com.example.boxwrapper.exception.AuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Box APIクライアント管理.
 *
 * <p>APIキーとBox SDK接続のマッピングを管理し、JWT認証またはDeveloper Token認証を
 * サポートします。複数のBox認証情報を管理し、ラウンドロビンでロードバランシングします。</p>
 *
 * <p>初期化時に設定ファイルから認証情報を読み込み、Box API接続を確立します。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@Component
public class BoxClientManager {

    private final BoxProperties boxProperties;
    private final ApiProperties apiProperties;
    private final ResourceLoader resourceLoader;

    private final Map<String, List<BoxAPIConnection>> apiKeyToConnections = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    public BoxClientManager(BoxProperties boxProperties,
                           ApiProperties apiProperties,
                           ResourceLoader resourceLoader) {
        this.boxProperties = boxProperties;
        this.apiProperties = apiProperties;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing Box client manager with {} API keys", apiProperties.getKeys().size());

        for (ApiProperties.ApiKeyConfig keyConfig : apiProperties.getKeys()) {
            try {
                initializeApiKey(keyConfig);
            } catch (Exception e) {
                log.error("Failed to initialize API key: {}", maskApiKey(keyConfig.getKey()), e);
            }
        }
    }

    /**
     * APIキーごとのBox接続を初期化
     */
    private void initializeApiKey(ApiProperties.ApiKeyConfig keyConfig) throws Exception {
        List<BoxAPIConnection> connections = new java.util.ArrayList<>();

        for (String configPath : keyConfig.getBoxConfigs()) {
            BoxAPIConnection connection = createConnection(configPath);
            connections.add(connection);
        }

        apiKeyToConnections.put(keyConfig.getKey(), connections);
        roundRobinCounters.put(keyConfig.getKey(), new AtomicInteger(0));

        log.info("Initialized API key {} with {} Box connections",
            maskApiKey(keyConfig.getKey()), connections.size());
    }

    /**
     * Box API接続を作成
     */
    private BoxAPIConnection createConnection(String configPath) throws Exception {
        if ("developer-token".equals(boxProperties.getAuth().getType())) {
            String token = boxProperties.getAuth().getDeveloperToken();
            if (token == null || token.isEmpty()) {
                throw new AuthenticationException("Developer token is not configured");
            }
            log.info("Creating Box connection with developer token");
            return new BoxAPIConnection(token);
        } else {
            // JWT authentication
            Resource resource = resourceLoader.getResource(configPath);
            if (!resource.exists()) {
                throw new AuthenticationException("Box config file not found: " + configPath);
            }

            try (InputStream configStream = resource.getInputStream();
                 InputStreamReader reader = new InputStreamReader(configStream)) {

                BoxConfig boxConfig = BoxConfig.readFrom(reader);
                IAccessTokenCache tokenCache = new InMemoryLRUAccessTokenCache(100);

                log.info("Creating Box connection with JWT from config: {}", configPath);
                return BoxDeveloperEditionAPIConnection.getAppEnterpriseConnection(
                    boxConfig, tokenCache
                );
            }
        }
    }

    /**
     * APIキーに対応するBox接続を取得
     */
    public BoxAPIConnection getConnection(String apiKey) {
        List<BoxAPIConnection> connections = apiKeyToConnections.get(apiKey);

        if (connections == null || connections.isEmpty()) {
            throw new AuthenticationException("Invalid API key or no Box connections configured");
        }

        if (connections.size() == 1) {
            return connections.get(0);
        }

        // Round-robin load balancing
        AtomicInteger counter = roundRobinCounters.get(apiKey);
        int index = counter.getAndIncrement() % connections.size();
        return connections.get(index);
    }

    /**
     * APIキーの検証
     */
    public boolean isValidApiKey(String apiKey) {
        return apiKeyToConnections.containsKey(apiKey);
    }

    /**
     * APIキーをマスク
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }
}
