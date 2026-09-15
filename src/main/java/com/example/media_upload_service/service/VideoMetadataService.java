//package com.example.media_upload_service.service;
//
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//
//@Service
//public class VideoMetadataService {
//
//    public Path removeMetadata(MultipartFile file) throws IOException {
//
//        Path inputFile = Files.createTempFile("input-", ".mp4");
//        Path outputFile = Files.createTempFile("output-", ".mp4");
//
//        try {
//
//            file.transferTo(inputFile);
//
//            ProcessBuilder processBuilder = new ProcessBuilder(
//                    "ffmpeg",
//                    "-i",
//                    inputFile.toString(),
//
//                    "-map_metadata",
//                    "-1",
//
//                    "-map_metadata:s:v",
//                    "-1",
//
//                    "-map_metadata:s:a",
//                    "-1",
//
//                    "-c",
//                    "copy",
//
//                    "-y",
//                    outputFile.toString()
//            );
//
//            processBuilder.redirectErrorStream(true);
//
//            Process process = processBuilder.start();
//
//            String output = new String(
//                    process.getInputStream().readAllBytes()
//            );
//
//            int exitCode = process.waitFor();
//
//            if (exitCode != 0) {
//
//                throw new IOException(
//                        "FFmpeg failed with exit code "
//                                + exitCode
//                                + "\nFFmpeg output:\n"
//                                + output
//                );
//            }
//
//            return outputFile;
//
//        } catch (InterruptedException e) {
//
//            Thread.currentThread().interrupt();
//
//            throw new IOException(
//                    "Video metadata removal was interrupted",
//                    e
//            );
//
//        } catch (Exception e) {
//
//            Files.deleteIfExists(outputFile);
//
//            throw new IOException(
//                    "Failed to remove MP4 metadata",
//                    e
//            );
//
//        } finally {
//
//            Files.deleteIfExists(inputFile);
//        }
//    }
//}
package com.example.media_upload_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class VideoMetadataService {

    public Path removeMetadata(MultipartFile file) throws IOException {

        Path inputFile = Files.createTempFile("input-", ".mp4");
        Path outputFile = Files.createTempFile("output-", ".mp4");

        try {

            file.transferTo(inputFile);

            log.info("Starting FFmpeg metadata removal for file: {}", file.getOriginalFilename());

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-i",
                    inputFile.toString(),

                    "-map_metadata",
                    "-1",

                    "-map_metadata:s:v",
                    "-1",

                    "-map_metadata:s:a",
                    "-1",

                    "-c",
                    "copy",

                    "-y",
                    outputFile.toString()
            );

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            String output = new String( process.getInputStream().readAllBytes() );

            int exitCode = process.waitFor();

            log.info("FFmpeg exit code: {}", exitCode);
            log.info("FFmpeg output:\n{}", output);

            if (exitCode != 0) {

                throw new IOException(
                        "FFmpeg failed with exit code "
                                + exitCode
                                + ": "
                                + output
                );
            }

            log.info("MP4 metadata removal completed successfully.");

            return outputFile;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IOException(
                    "Video metadata removal was interrupted",
                    e
            );

        } catch (Exception e) {

            Files.deleteIfExists(outputFile);

            throw new IOException(
                    "Failed to remove MP4 metadata",
                    e
            );

        } finally {

            Files.deleteIfExists(inputFile);
        }
    }
}