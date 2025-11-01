package com.example.boxwrapper.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * ロギングアスペクト
 * リクエスト/レスポンスのログ出力とリクエストIDトレーシング
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("within(com.example.boxwrapper.controller..*)")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String requestId = (String) request.getAttribute("requestId");

            if (requestId != null) {
                MDC.put("requestId", requestId);
            }

            long startTime = System.currentTimeMillis();

            try {
                log.info("Request: {} {} from {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr());

                Object result = joinPoint.proceed();

                long duration = System.currentTimeMillis() - startTime;
                log.info("Response: {} {} completed in {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    duration);

                return result;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("Request: {} {} failed after {}ms: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    duration,
                    e.getMessage());
                throw e;
            } finally {
                MDC.remove("requestId");
            }
        }

        return joinPoint.proceed();
    }

    @Around("within(com.example.boxwrapper.service..*)")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.debug("Service method called: {}.{}", className, methodName);

        try {
            Object result = joinPoint.proceed();
            log.debug("Service method completed: {}.{}", className, methodName);
            return result;
        } catch (Exception e) {
            log.error("Service method failed: {}.{} - {}", className, methodName, e.getMessage());
            throw e;
        }
    }
}
