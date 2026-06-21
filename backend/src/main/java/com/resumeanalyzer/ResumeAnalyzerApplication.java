package com.resumeanalyzer;

import com.resumeanalyzer.config.properties.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the AI Resume Analyzer &amp; Interview Coach API.
 *
 * <p>Bootstraps a Spring Boot 3 / Java 21 application that exposes a secured REST API
 * backing the resume analysis, skill-gap, and mock-interview features.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableJpaAuditing
@EnableAsync
public class ResumeAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeAnalyzerApplication.class, args);
    }
}
