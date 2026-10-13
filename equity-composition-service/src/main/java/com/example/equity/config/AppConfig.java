package com.example.equity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class AppConfig {

    @Bean
    public JsonFactory jsonFactory() {
        return JsonFactory.builder().build();
    }

    @Bean
    public JsonMapper jsonMapper(JsonFactory jsonFactory) {
        return JsonMapper.builder(jsonFactory)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
