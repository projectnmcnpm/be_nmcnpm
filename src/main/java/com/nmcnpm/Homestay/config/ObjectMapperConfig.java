package com.nmcnpm.Homestay.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expose một ObjectMapper bean dùng chung cho toàn app.
 * AuditAspect inject bean này để serialize DTO -> Map khi ghi AuditLog.
 *
 * Cấu hình:
 *  - JavaTimeModule: hỗ trợ serialize LocalDate, OffsetDateTime...
 *  - WRITE_DATES_AS_TIMESTAMPS = false: xuất date dạng ISO-8601 string
 */
@Configuration
public class ObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}