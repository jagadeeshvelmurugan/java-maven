package com.example.equity;

import com.example.equity.config.CompressionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CompressionProperties.class)
public class EquityCompositionApplication {
    public static void main(String[] args) {
        SpringApplication.run(EquityCompositionApplication.class, args);
    }
}
