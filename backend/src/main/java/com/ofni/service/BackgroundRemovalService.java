package com.ofni.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class BackgroundRemovalService {

    private final RestTemplate client;
    private final boolean enabled;

    public BackgroundRemovalService(
        @Value("${app.withoutbg.base-url:http://localhost:5000}") String baseUrl,
        @Value("${app.withoutbg.enabled:false}") boolean enabled
    ) {
        this.client = new RestTemplate();
        this.client.setUriTemplateHandler(new org.springframework.web.util.DefaultUriBuilderFactory(baseUrl));
        this.enabled = enabled;
    }

    public boolean removeBackground(Path sourcePath, Path destPath) {
        if (!enabled) return false;

        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            var body = new LinkedMultiValueMap<String, Object>();
            body.add("file", new FileSystemResource(sourcePath.toFile()));

            var entity = new HttpEntity<>(body, headers);
            var response = client.postForEntity("/api/remove", entity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Files.write(destPath, response.getBody());
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
