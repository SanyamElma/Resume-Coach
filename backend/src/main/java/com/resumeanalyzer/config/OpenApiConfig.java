package com.resumeanalyzer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 3 configuration. Registers the bearer-JWT security scheme so the
 * interactive docs can authenticate against secured endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI resumeAnalyzerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Resume Analyzer & Interview Coach API")
                        .description("REST API for resume analysis, skill-gap detection, and AI mock interviews.")
                        .version("1.0.0")
                        .contact(new Contact().name("Resume Analyzer Team").email("support@resume-analyzer.dev"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Provide the JWT access token returned by /api/auth/login")));
    }
}
