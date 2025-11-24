package com.mockly.core.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Service for MinIO operations: bucket management, pre-signed URLs, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinIOService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:mockly-artifacts}")
    private String bucketName;

    /**
     * Initialize bucket on application startup.
     * Creates bucket if it doesn't exist.
     */
    public void initializeBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            log.error("Failed to initialize MinIO bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to initialize MinIO bucket", e);
        }
    }

    /**
     * Generate pre-signed URL for uploading a file.
     *
     * @param objectName Object name (path) in the bucket
     * @param expiryTime Expiry time in seconds (default: 1 hour)
     * @return Pre-signed URL
     */
    public String generatePresignedUploadUrl(String objectName, int expiryTime) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryTime, TimeUnit.SECONDS)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            log.error("Failed to generate pre-signed upload URL for object: {}", objectName, e);
            throw new RuntimeException("Failed to generate pre-signed upload URL", e);
        }
    }

    /**
     * Generate pre-signed URL for downloading a file.
     *
     * @param objectName Object name (path) in the bucket
     * @param expiryTime Expiry time in seconds (default: 1 hour)
     * @return Pre-signed URL
     */
    public String generatePresignedDownloadUrl(String objectName, int expiryTime) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryTime, TimeUnit.SECONDS)
                            .build()
            );
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            log.error("Failed to generate pre-signed download URL for object: {}", objectName, e);
            throw new RuntimeException("Failed to generate pre-signed download URL", e);
        }
    }

    /**
     * Check if an object exists in the bucket.
     *
     * @param objectName Object name (path) in the bucket
     * @return true if object exists
     */
    public boolean objectExists(String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            log.error("Error checking object existence: {}", objectName, e);
            throw new RuntimeException("Failed to check object existence", e);
        } catch (InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException |
                 ServerException | XmlParserException e) {
            log.error("Error checking object existence: {}", objectName, e);
            throw new RuntimeException("Failed to check object existence", e);
        }
    }

    /**
     * Get object metadata.
     *
     * @param objectName Object name (path) in the bucket
     * @return Object metadata
     */
    public StatObjectResponse getObjectMetadata(String objectName) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | IOException |
                 NoSuchAlgorithmException | ServerException | XmlParserException e) {
            log.error("Failed to get object metadata for: {}", objectName, e);
            throw new RuntimeException("Failed to get object metadata", e);
        }
    }

    public String getBucketName() {
        return bucketName;
    }
}

