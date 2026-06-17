package com.ofni.service;

import org.bytedeco.javacpp.indexer.ByteIndexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_UNCHANGED;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

@Service
public class BackgroundRemovalService {

    private static final double MIN_FOREGROUND_RATIO = 0.15;

    private final RestClient client;
    private final boolean enabled;

    public BackgroundRemovalService(
        @Value("${app.withoutbg.base-url:http://localhost:5000}") String baseUrl,
        @Value("${app.withoutbg.enabled:false}") boolean enabled
    ) {
        this.client = RestClient.create(baseUrl);
        this.enabled = enabled;
    }

    public boolean removeBackground(Path sourcePath, Path destPath) {
        if (!enabled) {
            return false;
        }

        try {
            var body = new LinkedMultiValueMap<String, Object>();
            body.add("file", new FileSystemResource(sourcePath.toFile()));
            body.add("model", "u2net_cloth_seg");

            var response = client.post()
                .uri("/api/remove")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Files.write(destPath, response.getBody());
                if (!hasEnoughForeground(destPath)) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean hasEnoughForeground(Path imagePath) {
        try {
            var src = imread(imagePath.toString(), IMREAD_UNCHANGED);
            if (src.empty() || src.channels() != 4) {
                return true;
            }
            var channels = new org.bytedeco.opencv.opencv_core.MatVector();
            opencv_core.split(src, channels);
            var alpha = channels.get(3);
            var total = (double) alpha.total();
            var foreground = 0L;
            var idx = (ByteIndexer) alpha.createIndexer();
            for (long i = 0; i < total; i++) {
                if (idx.get(i) > 10) {
                    foreground++;
                }
            }
            idx.release();
            return (foreground / total) >= MIN_FOREGROUND_RATIO;
        } catch (Exception e) {
            return true;
        }
    }
}
