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
    private static final double MAX_WHITE_RATIO = 0.90;

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
                return hasEnoughForeground(destPath);
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean hasEnoughForeground(Path imagePath) {
        try {
            var src = imread(imagePath.toString(), IMREAD_UNCHANGED);
            if (src.empty()) {
                return false;
            }
            if (src.channels() != 4) {
                return false;
            }
            var channels = new org.bytedeco.opencv.opencv_core.MatVector();
            opencv_core.split(src, channels);
            var alpha = channels.get(3);
            var blue = channels.get(0);
            var green = channels.get(1);
            var red = channels.get(2);
            var total = (double) alpha.total();
            var foreground = 0L;
            var whitePixels = 0L;
            var alphaIdx = (ByteIndexer) alpha.createIndexer();
            var rIdx = (ByteIndexer) red.createIndexer();
            var gIdx = (ByteIndexer) green.createIndexer();
            var bIdx = (ByteIndexer) blue.createIndexer();
            for (long i = 0; i < total; i++) {
                if (alphaIdx.get(i) > 10) {
                    foreground++;
                    if (rIdx.get(i) > 240 && gIdx.get(i) > 240 && bIdx.get(i) > 240) {
                        whitePixels++;
                    }
                }
            }
            alphaIdx.release();
            rIdx.release();
            gIdx.release();
            bIdx.release();
            if (foreground == 0) {
                return false;
            }
            if ((double) whitePixels / foreground > MAX_WHITE_RATIO) {
                return false;
            }
            return (foreground / total) >= MIN_FOREGROUND_RATIO;
        } catch (Exception e) {
            return false;
        }
    }
}
