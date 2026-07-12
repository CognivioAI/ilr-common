package com.cognivio.ai.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Registers the shared {@link CommonExceptionHandler}. Backs off if the service
 * already defines its own {@code CommonExceptionHandler} bean.
 */
@AutoConfiguration
@ConditionalOnClass(RestControllerAdvice.class)
public class IlrWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CommonExceptionHandler ilrCommonExceptionHandler() {
        return new CommonExceptionHandler();
    }
}
