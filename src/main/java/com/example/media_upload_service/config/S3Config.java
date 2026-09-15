package com.example.media_upload_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

@Configuration
@Slf4j
public class S3Config {

    @Value("${app.s3.bucket}")
    private String bucketName;


    @Bean
    @ConditionalOnProperty(name = "app.s3.create-bucket-on-startup", havingValue = "true")
    public CommandLineRunner createBucket(S3Client s3Client) {
        return args -> {
            try {
                log.info("Checking/creating S3 bucket: {}", bucketName);
                s3Client.createBucket(
                        CreateBucketRequest.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Created S3 bucket: {}", bucketName);
            } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
                log.info("S3 bucket '{}' already exists", bucketName);
            } catch (Exception e) {
                log.warn("Could not automatically create S3 bucket '{}': {}. Proceeding assuming bucket exists.",
                        bucketName, e.getMessage());
            }
        };
    }
}