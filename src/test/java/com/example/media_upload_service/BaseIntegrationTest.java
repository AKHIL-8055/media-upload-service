package com.example.media_upload_service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("media_test_db")
            .withUsername("testuser")
            .withPassword("testpass");


        @Container
        protected static final LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
                .withServices(LocalStackContainer.Service.S3);




        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("spring.flyway.baseline-on-migrate", () -> "true");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

            registry.add("spring.cloud.aws.region.static", localstack::getRegion);
            registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
            registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
            registry.add("spring.cloud.aws.s3.endpoint", () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
            registry.add("spring.cloud.aws.s3.path-style-access-enabled", () -> "true");

            registry.add("app.s3.bucket", () -> "integration-test-bucket");
            registry.add("app.s3.create-bucket-on-startup", () -> "true");
    }
}
