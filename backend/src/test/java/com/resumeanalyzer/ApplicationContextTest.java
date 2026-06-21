package com.resumeanalyzer;

import com.resumeanalyzer.ai.AiProviderResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: boots the full Spring context against in-memory H2 to verify that every
 * bean (security, JPA, AI providers, controllers, the data seeder) wires up correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest {

    @Autowired
    private AiProviderResolver aiProviderResolver;

    @Test
    void contextLoads_andMockProviderIsActive() {
        assertThat(aiProviderResolver).isNotNull();
        assertThat(aiProviderResolver.current().name()).isEqualTo("mock");
    }
}
