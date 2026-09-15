package com.example.media_upload_service;

import com.example.media_upload_service.entity.Media;
import com.example.media_upload_service.repository.MediaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class MediaIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ObjectMapper objectMapper;




    @Test
    @DisplayName("End-to-End: Upload file, persist in Postgres, verify in S3, and download")
    void testUploadAndDownloadFlow() throws Exception {

        String originalContent =
                "Hello from integration test with Testcontainers and Spring Cloud AWS!";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample-contract.txt",
                "text/plain",
                originalContent.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(
                        multipart("/media/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fileName").value("sample-contract.txt"))
                .andExpect(jsonPath("$.fileSize").value(
                        originalContent.getBytes(StandardCharsets.UTF_8).length))
                .andExpect(jsonPath("$.s3FileName").isNotEmpty())
                .andReturn();

        Media uploadedMedia = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(),
                Media.class
        );

        Long mediaId = uploadedMedia.getId();

        assertThat(mediaRepository.findById(mediaId)).isPresent();

        Media dbMedia = mediaRepository.findById(mediaId).get();

        assertThat(dbMedia.getFileName())
                .isEqualTo("sample-contract.txt");

        mockMvc.perform(get("/media/{id}/download", mediaId))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"sample-contract.txt\""))
                .andExpect(header().string(
                        "Content-Type",
                        "application/octet-stream"))
                .andExpect(header().string(
                        "Content-Length",
                        String.valueOf(
                                originalContent.getBytes(StandardCharsets.UTF_8).length)))
                .andExpect(content().string(originalContent));

        mockMvc.perform(get("/media/{id}", mediaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mediaId))
                .andExpect(jsonPath("$.fileName").value("sample-contract.txt"));
    }




    @Test
    @DisplayName("Upload JPEG and verify EXIF metadata is removed")
    void testJpegMetadataIsRemoved() throws Exception {

        String imageName = "test-image-with-real-exif.jpg";

        ClassPathResource resource =
                new ClassPathResource("images/" + imageName);

        byte[] originalImage = resource.getInputStream().readAllBytes();


        ImageMetadata originalMetadata =
                Imaging.getMetadata(originalImage);

        assertThat(originalMetadata).isNotNull();

        assertThat(originalMetadata instanceof JpegImageMetadata)
                .isTrue();

        JpegImageMetadata jpegMetadata =
                (JpegImageMetadata) originalMetadata;

        assertThat(jpegMetadata.getExif())
                .as("Test JPEG should contain EXIF metadata")
                .isNotNull();


        MockMultipartFile file = new MockMultipartFile(
                "file",
                imageName,
                "image/jpeg",
                originalImage
        );


        MvcResult uploadResult = mockMvc.perform(
                        multipart("/media/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value(imageName))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();


        Media uploadedMedia = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(),
                Media.class
        );

        Long mediaId = uploadedMedia.getId();


        MvcResult downloadResult = mockMvc.perform(
                        get("/media/{id}/download", mediaId))
                .andExpect(status().isOk())
                .andReturn();

        byte[] cleanedImage =
                downloadResult.getResponse().getContentAsByteArray();


        ImageMetadata cleanedMetadata =
                Imaging.getMetadata(cleanedImage);

        if (cleanedMetadata instanceof JpegImageMetadata) {

            JpegImageMetadata cleanedJpegMetadata =
                    (JpegImageMetadata) cleanedMetadata;

            assertThat(cleanedJpegMetadata.getExif())
                    .as("EXIF metadata should be removed from JPEG")
                    .isNull();
        }
    }


    @Test
    @DisplayName("Upload PNG and verify metadata chunks are removed")
    void testPngMetadataIsRemoved() throws Exception {

        String imageName = "real_metadata_test.png";

        ClassPathResource resource =
                new ClassPathResource("images/" + imageName);

        byte[] originalImage = resource.getInputStream().readAllBytes();


        Set<String> originalMetadataChunks =
                findPngMetadataChunks(originalImage);

        assertThat(originalMetadataChunks)
                .as("Test PNG should contain metadata")
                .isNotEmpty();


        MockMultipartFile file = new MockMultipartFile(
                "file",
                imageName,
                "image/png",
                originalImage
        );


        MvcResult uploadResult = mockMvc.perform(
                        multipart("/media/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value(imageName))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();


        Media uploadedMedia = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(),
                Media.class
        );

        Long mediaId = uploadedMedia.getId();


        MvcResult downloadResult = mockMvc.perform(
                        get("/media/{id}/download", mediaId))
                .andExpect(status().isOk())
                .andReturn();

        byte[] cleanedImage =
                downloadResult.getResponse().getContentAsByteArray();


        Set<String> cleanedMetadataChunks =
                findPngMetadataChunks(cleanedImage);


        assertThat(cleanedMetadataChunks)
                .as("PNG metadata should be removed")
                .isEmpty();
    }


    @Test
    @DisplayName("Upload MP4 and verify metadata is removed")
    void testMp4MetadataIsRemoved() throws Exception {

        String videoName = "metadata-test-video.mp4";

        ClassPathResource resource =
                new ClassPathResource("videos/" + videoName);

        byte[] originalVideo =
                resource.getInputStream().readAllBytes();


        Path originalVideoFile =
                Files.createTempFile("original-video-", ".mp4");

        Files.write(originalVideoFile, originalVideo);

        try {

            String originalMetadata =
                    getMp4Metadata(originalVideoFile);

            assertThat(originalMetadata)
                    .as("Test MP4 should contain metadata")
                    .isNotBlank();


            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    videoName,
                    "video/mp4",
                    originalVideo
            );

            MvcResult uploadResult = mockMvc.perform(
                            multipart("/media/upload").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fileName").value(videoName))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andReturn();


            Media uploadedMedia = objectMapper.readValue(
                    uploadResult.getResponse().getContentAsString(),
                    Media.class
            );

            Long mediaId = uploadedMedia.getId();


            MvcResult downloadResult = mockMvc.perform(
                            get("/media/{id}/download", mediaId))
                    .andExpect(status().isOk())
                    .andReturn();

            byte[] cleanedVideo =
                    downloadResult.getResponse().getContentAsByteArray();


            Path cleanedVideoFile =
                    Files.createTempFile("cleaned-video-", ".mp4");

            Files.write(cleanedVideoFile, cleanedVideo);

            try {

                String cleanedMetadata =
                        getMp4Metadata(cleanedVideoFile);

                assertThat(cleanedMetadata)
                        .as("Original MP4 metadata should be removed")
                        .doesNotContain(originalMetadata);

            } finally {
                Files.deleteIfExists(cleanedVideoFile);
            }

        } finally {
            Files.deleteIfExists(originalVideoFile);
        }
    }




    @Test
    @DisplayName("Upload different JPEG images successfully")
    void testUploadJpegImages() throws Exception {

        String[] imageNames = {
                "test-image-with-real-exif.jpg"
        };

        for (String imageName : imageNames) {

            ClassPathResource resource =
                    new ClassPathResource("images/" + imageName);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    imageName,
                    "image/jpeg",
                    resource.getInputStream()
            );

            mockMvc.perform(multipart("/media/upload").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.fileName").value(imageName))
                    .andExpect(jsonPath("$.fileSize").isNumber())
                    .andExpect(jsonPath("$.s3FileName").isNotEmpty());
        }
    }




    @Test
    @DisplayName("Upload different PNG images successfully")
    void testUploadPngImages() throws Exception {

        String[] imageNames = {
                "real_metadata_test.png"
        };

        for (String imageName : imageNames) {

            ClassPathResource resource =
                    new ClassPathResource("images/" + imageName);

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    imageName,
                    "image/png",
                    resource.getInputStream()
            );

            mockMvc.perform(multipart("/media/upload").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.fileName").value(imageName))
                    .andExpect(jsonPath("$.fileSize").isNumber())
                    .andExpect(jsonPath("$.s3FileName").isNotEmpty());
        }
    }



    @Test
    @DisplayName("Failure Path: Download with non-existent ID returns 404 NOT_FOUND")
    void testDownloadNonExistentId_Returns404() throws Exception {

        long nonExistentId = 999999L;

        mockMvc.perform(get("/media/{id}/download", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEDIA_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value(containsString(
                                "Media not found with id: " + nonExistentId)))
                .andExpect(jsonPath("$.path")
                        .value("/media/" + nonExistentId + "/download"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }




    @Test
    @DisplayName("Failure Path: Upload empty file returns 400 BAD_REQUEST")
    void testUploadEmptyFile_Returns400() throws Exception {

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/media/upload").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Cannot upload empty file"))
                .andExpect(jsonPath("$.path").value("/media/upload"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }




    @Test
    @DisplayName("Failure Path: Media exists in DB but missing from S3 returns 404 MEDIA_FILE_NOT_FOUND")
    void testDownloadMissingS3Key_Returns404() throws Exception {

        Media orphanMedia = mediaRepository.save(
                new Media(
                        "missing.txt",
                        123L,
                        "non-existent-s3-key"
                )
        );

        mockMvc.perform(get("/media/{id}/download", orphanMedia.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEDIA_FILE_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Media file not found in storage"))
                .andExpect(jsonPath("$.path")
                        .value("/media/" + orphanMedia.getId() + "/download"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }



    private String getMp4Metadata(Path videoFile) throws Exception {

        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffprobe",
                "-v",
                "quiet",
                "-show_entries",
                "format_tags:stream_tags",
                "-of",
                "default=noprint_wrappers=1",
                videoFile.toString()
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        int exitCode = process.waitFor();

        assertThat(exitCode)
                .as("FFprobe should successfully inspect the MP4")
                .isZero();

        return output.trim();
    }





    private Set<String> findPngMetadataChunks(byte[] pngBytes)
            throws IOException {

        Set<String> metadataChunks = new HashSet<>();

        if (pngBytes.length < 8) {
            return metadataChunks;
        }


        byte[] pngSignature = {
                (byte) 137,
                80,
                78,
                71,
                13,
                10,
                26,
                10
        };

        if (!Arrays.equals(
                Arrays.copyOfRange(pngBytes, 0, 8),
                pngSignature)) {

            throw new IOException("Invalid PNG file");
        }

        int position = 8;

        while (position + 12 <= pngBytes.length) {

            int length =
                    ((pngBytes[position] & 0xff) << 24)
                            | ((pngBytes[position + 1] & 0xff) << 16)
                            | ((pngBytes[position + 2] & 0xff) << 8)
                            | (pngBytes[position + 3] & 0xff);

            String chunkType = new String(
                    pngBytes,
                    position + 4,
                    4,
                    StandardCharsets.US_ASCII
            );



            if (chunkType.equals("tEXt")
                    || chunkType.equals("zTXt")
                    || chunkType.equals("iTXt")
                    || chunkType.equals("eXIf")
                    || chunkType.equals("iCCP")
                    || chunkType.equals("pHYs")
                    || chunkType.equals("tIME")) {

                metadataChunks.add(chunkType);
            }

            position += 12 + length;

            if (chunkType.equals("IEND")) {
                break;
            }

            if (position > pngBytes.length) {
                break;
            }
        }

        return metadataChunks;
    }
}