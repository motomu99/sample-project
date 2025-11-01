package com.example.boxwrapper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * API Key設定プロパティ
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiProperties {

    private List<ApiKeyConfig> keys = new ArrayList<>();

    @Data
    public static class ApiKeyConfig {
        private String key;
        private List<String> boxConfigs = new ArrayList<>();
        private String loadBalance = "round-robin";  // round-robin or random
    }
}
