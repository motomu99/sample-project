package com.example.boxwrapper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Box SDK設定プロパティ
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "box")
public class BoxProperties {

    private Auth auth = new Auth();
    private RateLimit rateLimit = new RateLimit();
    private Retry retry = new Retry();

    @Data
    public static class Auth {
        private String type = "jwt";  // jwt or developer-token
        private String configFile;
        private String developerToken;
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerSecond = 10;
        private boolean adaptive = true;
    }

    @Data
    public static class Retry {
        private boolean onRateLimit = true;
        private int maxAttempts = 5;
        private boolean respectRetryAfter = true;
    }
}
