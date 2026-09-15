package com.example.media_upload_service.config;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponseCustomizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.Instant;

@Configuration
public class ErrorHandlingConfig {

    @Bean
    public ApiErrorResponseCustomizer errorResponseCustomizer(ObjectProvider<HttpServletRequest> requestProvider) {
        return response -> {
            response.addErrorProperty("timestamp", Instant.now());

            HttpStatusCode status = response.getHttpStatus();
            if (status != null) {
                HttpStatus httpStatus = HttpStatus.resolve(status.value());
                if (httpStatus != null) {
                    response.addErrorProperty("error", httpStatus.getReasonPhrase());
                }
            }

            HttpServletRequest request = requestProvider.getIfAvailable();
            if (request != null) {
                response.addErrorProperty("path", request.getRequestURI());
            }
        };
    }
}











