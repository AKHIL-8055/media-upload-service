package com.example.media_upload_service.service;

import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngReader;
import ar.com.hjg.pngj.PngWriter;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class ImageMetadataService {


    public byte[] removeJpegMetadata(MultipartFile file) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            new ExifRewriter().removeExifMetadata(
                    file.getBytes(),
                    outputStream
            );

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new IOException("Failed to remove JPEG metadata", e);
        }
    }


    public byte[] removePngMetadata(MultipartFile file) throws IOException {

        File inputFile = File.createTempFile("input-", ".png");
        File outputFile = File.createTempFile("output-", ".png");

        PngReader pngReader = null;
        PngWriter pngWriter = null;

        try {


            file.transferTo(inputFile);

            pngReader = new PngReader(inputFile);


            pngWriter = new PngWriter(outputFile,pngReader.imgInfo,true);

            for (int row = 0; row < pngReader.imgInfo.rows; row++) {

                ImageLineInt imageLine =
                        (ImageLineInt) pngReader.readRow();

                pngWriter.writeRow(imageLine);
            }

            pngReader.end();
            pngReader = null;

            pngWriter.end();
            pngWriter = null;

            return Files.readAllBytes(outputFile.toPath());

        } catch (Exception e) {
            throw new IOException("Failed to remove PNG metadata", e);

        } finally {

            if (pngReader != null) {
                try {
                    pngReader.end();
                } catch (Exception ignored) {
                }
            }

            if (pngWriter != null) {
                try {
                    pngWriter.end();
                } catch (Exception ignored) {
                }
            }

            Files.deleteIfExists(inputFile.toPath());
            Files.deleteIfExists(outputFile.toPath());
        }
    }
}