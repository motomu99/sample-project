package com.example.boxwrapper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 非同期処理設定プロパティ
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "async")
public class AsyncProperties {

    private Parallel parallel = new Parallel();
    private ThreadPool threadPool = new ThreadPool();

    @Data
    public static class Parallel {
        private int maxConcurrentUploads = 5;
        private int maxConcurrentDownloads = 5;
        private int semaphoreTimeoutSeconds = 30;
    }

    @Data
    public static class ThreadPool {
        private int coreSize = 10;
        private int maxSize = 20;
        private int queueCapacity = 50;
        private String threadNamePrefix = "async-box-";
    }
}
