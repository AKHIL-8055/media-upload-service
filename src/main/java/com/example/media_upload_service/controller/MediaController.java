package com.example.media_upload_service.controller;

import com.example.media_upload_service.dto.DownloadedMedia;
import com.example.media_upload_service.entity.Media;
import com.example.media_upload_service.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/media")
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }







    @PostMapping("/upload")
    public ResponseEntity<Media> upload(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("REST request to upload file: {}", file != null ? file.getOriginalFilename() : "null");
        Media media = mediaService.upload(file);
        return ResponseEntity.ok(media);
    }






    

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        log.info("REST request to download media with ID: {}", id);
        DownloadedMedia media = mediaService.download(id);

        Resource resource = new InputStreamResource(media.getInputStream()) {
            @Override
            public long contentLength() {
                return media.getContentLength() != null ? media.getContentLength() : -1;
            }

            @Override
            public String getFilename() {
                return media.getFileName();
            }
        };

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + media.getFileName() + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM);

        if (media.getContentLength() != null) {
            responseBuilder.contentLength(media.getContentLength());
        }

        return responseBuilder.body(resource);
    }










    @GetMapping("/{id}")
    public ResponseEntity<Media> getMedia(@PathVariable Long id) {
        log.info("REST request to get metadata for media ID: {}", id);
        return ResponseEntity.ok(mediaService.getMedia(id));
    }
}