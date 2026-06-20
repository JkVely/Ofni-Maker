package com.ofni.service;

import com.ofni.dto.ClothResponse;
import com.ofni.model.Occasion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OllamaClient {

    private final RestClient client;
    private final String model;

    public OllamaClient(
        @Value("${app.ollama.base-url}") String baseUrl,
        @Value("${app.ollama.model}") String model
    ) {
        this.client = RestClient.create(baseUrl);
        this.model = model;
    }

    /** Pregunta a la IA de que material esta hecha la prenda viendo la foto. */
    public String analyzeMaterial(String imagePath) {
        var b64 = encodeImage(imagePath);
        var prompt = """
            Eres un experto en moda y textiles. Observa esta prenda de ropa.
            Responde SOLO con el nombre del material principal en espanol,
            en una sola palabra: ej: "algodon", "lana", "poliester", "cuero", "seda", etc.
            Si no estas seguro, responde "poliester".
            """;

        var response = client.post()
            .uri("/api/chat")
            .body(Map.of(
                "model", model,
                "messages", List.of(Map.of(
                    "role", "user",
                    "content", prompt,
                    "images", List.of(b64)
                )),
                "stream", false
            ))
            .retrieve()
            .body(Map.class);

        return extractChatResponse(response);
    }

    @SuppressWarnings("unchecked")
    public String generateOutfit(
        List<ClothResponse> availableClothes,
        Occasion occasion,
        Double temperature
    ) {
        var clothesDesc = new StringBuilder();
        for (var c : availableClothes) {
            clothesDesc.append("- %s (%s, %s, colores: %s%n)"
                .formatted(c.name(), c.category(), c.slot(), c.colorPalette()));
        }

        var b64Images = availableClothes.stream()
            .map(c -> c.processedImagePath())
            .map(this::encodeImage)
            .toList();

        var prompt = """
            Eres un asesor de moda personal. Tu tarea es crear el MEJOR outfit
            posible con las prendas disponibles.

            OCASION: %s
            TEMPERATURA: %.1f°C

            PRENDAS DISPONIBLES:
            %s

            Las prendas tienen colores HEX listados. Debes priorizar:
            1. Combinacion armonica de colores
            2. Adecuacion a la ocasion
            3. Adecuacion a la temperatura
            4. Coherencia de silueta y estilo

            Responde SOLO con los IDs de las prendas elegidas en formato JSON:
            {"selected_ids": [1, 5, 8, 12], "name": "Nombre creativo del outfit"}
            """.formatted(occasion, temperature, clothesDesc.toString());

        var response = client.post()
            .uri("/api/chat")
            .body(Map.of(
                "model", model,
                "messages", List.of(Map.of(
                    "role", "user",
                    "content", prompt,
                    "images", b64Images
                )),
                "stream", false
            ))
            .retrieve()
            .body(Map.class);

        return extractChatResponse(response);
    }

    public record OllamaCategory(String name, String slot) {}

    public OllamaCategory classifyItem(String imagePath) {
        var b64 = encodeImage(imagePath);
        var prompt = """
            Eres un experto en moda. Mira esta imagen y responde SOLO con
            una categoria de las siguientes (elige la mas cercana):
            TSHIRT, SHIRT, POLO, BLOUSE, SWEATER, HOODIE, JACKET, COAT,
            PANTS, JEANS, SHORTS, SKIRT, DRESS,
            SHOES, SNEAKERS, BOOTS, SANDALS,
            HAT, SCARF, BELT, BAG, ACCESSORY, OTHER.

            Formato exacto: CATEGORIA
            """;

        var response = client.post()
            .uri("/api/chat")
            .body(Map.of(
                "model", model,
                "messages", List.of(Map.of(
                    "role", "user",
                    "content", prompt,
                    "images", List.of(b64)
                )),
                "stream", false
            ))
            .retrieve()
            .body(Map.class);

        var text = extractChatResponse(response).trim().toUpperCase();
        return new OllamaCategory(text, inferSlot(text));
    }

    private String inferSlot(String category) {
        return switch (category) {
            case "TSHIRT", "SHIRT", "POLO", "BLOUSE", "SWEATER", "HOODIE", "DRESS" -> "TOP";
            case "JACKET", "COAT" -> "OUTERWEAR";
            case "PANTS", "JEANS", "SHORTS", "SKIRT" -> "BOTTOM";
            case "SHOES", "SNEAKERS", "BOOTS", "SANDALS" -> "FOOTWEAR";
            case "HAT" -> "HEAD";
            default -> "ACCESSORY";
        };
    }

    private String encodeImage(String path) {
        try {
            var bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to encode image: " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractChatResponse(Map<?, ?> response) {
        if (response == null) return "";
        var message = (Map<?, ?>) response.get("message");
        if (message != null && message.containsKey("content")) {
            return (String) message.get("content");
        }
        return "";
    }
}
