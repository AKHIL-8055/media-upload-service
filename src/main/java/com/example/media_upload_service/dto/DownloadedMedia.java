package com.example.media_upload_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadedMedia implements Closeable {
    private String fileName;
    private Long contentLength;
    private InputStream inputStream;

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
