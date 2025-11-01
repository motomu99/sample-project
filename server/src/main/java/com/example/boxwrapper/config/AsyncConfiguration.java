package com.example.boxwrapper.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * 非同期処理設定
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AsyncConfiguration implements AsyncConfigurer {

    private final AsyncProperties asyncProperties;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getThreadPool().getCoreSize());
        executor.setMaxPoolSize(asyncProperties.getThreadPool().getMaxSize());
        executor.setQueueCapacity(asyncProperties.getThreadPool().getQueueCapacity());
        executor.setThreadNamePrefix(asyncProperties.getThreadPool().getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Async executor configured with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
            asyncProperties.getThreadPool().getCoreSize(),
            asyncProperties.getThreadPool().getMaxSize(),
            asyncProperties.getThreadPool().getQueueCapacity());

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("Uncaught async exception in method: {}", method.getName(), ex);
            }
        };
    }
}
