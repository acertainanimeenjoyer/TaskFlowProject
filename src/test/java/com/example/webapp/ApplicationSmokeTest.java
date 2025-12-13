package com.example.webapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test to verify Spring Boot application context loads successfully
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/testdb",
        "jwt.secret=test-secret-key-for-smoke-tests-must-be-long-enough",
        "jwt.expiration=3600000"
})
class ApplicationSmokeTest {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // If all beans are properly configured, this test will pass
        assertTrue(true, "Application context loaded successfully");
    }

    @Test
    void applicationStartsSuccessfully() {
        // Verifies that all components, services, and repositories are wired correctly
        assertDoesNotThrow(() -> {
            // Application started successfully
        });
    }
}
