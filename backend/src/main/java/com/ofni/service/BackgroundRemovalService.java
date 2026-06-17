package com.ofni.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class BackgroundRemovalService {

    private final RestClient client;
    private final boolean enabled;

    public BackgroundRemovalService(
        @Value("${app.withoutbg.base-url:http://localhost:5000}") String baseUrl,
        @Value("${app.withoutbg.enabled:false}") boolean enabled
    ) {
        this.client = RestClient.create(baseUrl);
        this.enabled = enabled;
    }

    /**
     * Quita el fondo de una imagen using the withoutbg Docker service.
     * @param sourcePath ruta de la imagen original
     * @param destPath   ruta donde guardar la imagen sin fondo
     * @return true si se proceso correctamente, false si el servicio no esta disponible
     */
    public boolean removeBackground(Path sourcePath, Path destPath) {
        if (!enabled) {
            return false;
        }

        try {
            var body = new LinkedMultiValueMap<String, Object>();
            body.add("image", new FileSystemResource(sourcePath.toFile()));

            var response = client.post()
                .uri("/api/remove")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Files.write(destPath, response.getBody());
                return true;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }
}
