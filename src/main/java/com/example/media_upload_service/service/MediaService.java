package com.example.media_upload_service.service;

import com.example.media_upload_service.dto.DownloadedMedia;
import com.example.media_upload_service.entity.Media;
import com.example.media_upload_service.exception.MediaNotFoundException;
import com.example.media_upload_service.repository.MediaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Slf4j
public class MediaService {

    private final S3Client s3Client;
    private final MediaRepository mediaRepository;
    private final ImageMetadataService imageMetadataService;
    private final VideoMetadataService videoMetadataService;

    @Value("${app.s3.bucket}")
    private String bucketName;


    public MediaService(
            S3Client s3Client,
            MediaRepository mediaRepository,
            ImageMetadataService imageMetadataService,
            VideoMetadataService videoMetadataService
    ) {
        this.s3Client = s3Client;
        this.mediaRepository = mediaRepository;
        this.imageMetadataService = imageMetadataService;
        this.videoMetadataService = videoMetadataService;
    }


    public Media upload(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        String originalFilename = file.getOriginalFilename();

        String fileName = (originalFilename != null && !originalFilename.isBlank())
                ? originalFilename
                : "unknown";

        String s3FileName = UUID.randomUUID() + "-" + fileName;

        log.info(
                "Processing file '{}' ({} bytes)",
                fileName,
                file.getSize()
        );


        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3FileName)
                .contentType(file.getContentType())
                .build();

        Media saved = null;

        if (isJpeg(file)) {

            log.info("JPG/JPEG image detected. Removing EXIF metadata.");

            byte[] fileBytes = imageMetadataService.removeJpegMetadata(file);

            log.info(
                    "Image metadata removed. Original size: {} bytes, cleaned size: {} bytes",
                    file.getSize(),
                    fileBytes.length
            );

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(fileBytes)
            );

            saved = saveMedia(
                    fileName,
                    fileBytes.length,
                    s3FileName
            );

        } else if (isPng(file)) {

            log.info("PNG image detected. Removing PNG metadata.");

            byte[] fileBytes = imageMetadataService.removePngMetadata(file);

            log.info(
                    "PNG metadata removed. Original size: {} bytes, cleaned size: {} bytes",
                    file.getSize(),
                    fileBytes.length
            );

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(fileBytes)
            );

            saved = saveMedia(
                    fileName,
                    fileBytes.length,
                    s3FileName
            );

        } else if (isMp4(file)) {

            log.info("MP4 video detected. Removing video metadata.");

            Path cleanedVideo = null;

            try {

                cleanedVideo = videoMetadataService.removeMetadata(file);

                long cleanedSize = Files.size(cleanedVideo);

                log.info(
                        "Video metadata removed. Original size: {} bytes, cleaned size: {} bytes",
                        file.getSize(),
                        cleanedSize
                );

                s3Client.putObject(
                        request,
                        RequestBody.fromFile(cleanedVideo)
                );

                saved = saveMedia(
                        fileName,
                        cleanedSize,
                        s3FileName
                );

            } finally {

                if (cleanedVideo != null) {
                    Files.deleteIfExists(cleanedVideo);
                }
            }

        } else {

            log.info(
                    "File '{}' is not a supported media type. Uploading unchanged.",
                    fileName
            );

            byte[] fileBytes = file.getBytes();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(fileBytes)
            );

            saved = saveMedia(
                    fileName,
                    fileBytes.length,
                    s3FileName
            );
        }


        log.info(
                "Uploaded file '{}' to S3 bucket '{}'",
                fileName,
                bucketName
        );

        log.info(
                "Successfully persisted media ID: {}, S3 key: {}",
                saved.getId(),
                s3FileName
        );

        return saved;
    }


    private Media saveMedia(
            String fileName,
            long fileSize,
            String s3FileName
    ) {

        Media media = new Media(
                fileName,
                fileSize,
                s3FileName
        );

        return mediaRepository.save(media);
    }


    private boolean isJpeg(MultipartFile file) {

        String contentType = file.getContentType();

        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return true;
        }

        String fileName = file.getOriginalFilename();

        return fileName != null &&
                (
                        fileName.toLowerCase().endsWith(".jpg") ||
                                fileName.toLowerCase().endsWith(".jpeg")
                );
    }


    private boolean isPng(MultipartFile file) {

        String contentType = file.getContentType();

        if ("image/png".equalsIgnoreCase(contentType)) {
            return true;
        }

        String fileName = file.getOriginalFilename();

        return fileName != null &&
                fileName.toLowerCase().endsWith(".png");
    }


    private boolean isMp4(MultipartFile file) {

        String contentType = file.getContentType();

        if ("video/mp4".equalsIgnoreCase(contentType)) {
            return true;
        }

        String fileName = file.getOriginalFilename();

        return fileName != null &&
                fileName.toLowerCase().endsWith(".mp4");
    }


    public DownloadedMedia download(Long id) {

        log.info("Downloading media with ID: {}", id);

        Media media = mediaRepository.findById(id)
                .orElseThrow(() ->
                        new MediaNotFoundException(
                                "Media not found with id: " + id
                        )
                );

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(media.getS3FileName())
                .build();

        ResponseInputStream<GetObjectResponse> inputStream =
                s3Client.getObject(request);

        GetObjectResponse response = inputStream.response();

        log.info(
                "Successfully retrieved file '{}' for media ID: {}",
                media.getFileName(),
                id
        );

        return new DownloadedMedia(
                media.getFileName(),
                response.contentLength(),
                inputStream
        );
    }


    public Media getMedia(Long id) {

        return mediaRepository.findById(id)
                .orElseThrow(() ->
                        new MediaNotFoundException(
                                "Media not found with id: " + id
                        )
                );
    }
}