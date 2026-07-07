package com.ofni.service;

import com.ofni.dto.ClothResponse;
import com.ofni.model.Category;
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
    private final String visionModel;
    private final String fastModel;

    public OllamaClient(
        @Value("${app.ollama.base-url}") String baseUrl,
        @Value("${app.ollama.model}") String visionModel,
        @Value("${app.ollama.model-fast}") String fastModel
    ) {
        this.client = RestClient.create(baseUrl);
        this.visionModel = visionModel;
        this.fastModel = fastModel;
    }

    public String analyzeMaterial(String imagePath, Category category) {
        var prompt = """
            Eres un experto textil. Mira esta foto de una prenda y determina
            su material principal observando la textura de la tela.

            TIPO DETECTADO: %s

            Responde SOLO con el material en espanol, en UNA SOLA PALABRA:
            - Telas comunes: algodon, lino, seda, lana, denim, cuero,
              poliester, viscosa, acrilico, nailon, gamuza, lona, caucho,
              plumon, cachemira, polar, lyocell, spandex, terciopelo

            IMPORTANTE: Mira la textura de la tela en la imagen.
            NO respondas "poliester" por defecto.
            Responde solo la palabra del material, nada mas.
            """.formatted(categoriaEnEspanol(category));

        return chatWithImage(imagePath, prompt);
    }

    private String categoriaEnEspanol(Category cat) {
        return switch (cat) {
            case TSHIRT -> "Remera";
            case SHIRT -> "Camisa";
            case POLO -> "Polo";
            case BLOUSE -> "Blusa";
            case SWEATER -> "Sueter";
            case HOODIE -> "Buzo";
            case JACKET -> "Chaqueta";
            case COAT -> "Abrigo";
            case PANTS -> "Pantalon";
            case JEANS -> "Jean";
            case SHORTS -> "Short";
            case SKIRT -> "Falda";
            case DRESS -> "Vestido";
            case SHOES, SNEAKERS, BOOTS, SANDALS -> "Calzado";
            case HAT -> "Gorro";
            case SCARF -> "Bufanda";
            case BELT -> "Cinturon";
            case BAG -> "Bolso";
            case ACCESSORY, OTHER -> "Accesorio";
        };
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
                "model", visionModel,
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

    private String chatWithImage(String imagePath, String prompt) {
        var b64 = encodeImage(imagePath);
        try {
            var response = client.post()
                .uri("/api/chat")
                .body(Map.of(
                    "model", visionModel,
                    "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt,
                        "images", List.of(b64)
                    )),
                    "stream", false
                ))
                .retrieve()
                .body(Map.class);

            var text = extractChatResponse(response);
            if (text != null && !text.isBlank()) {
                var cleaned = text.toLowerCase()
                    .replaceAll("[^a-záéíóúñ]", "").trim();
                if (cleaned.length() <= 20 && !cleaned.isEmpty()) {
                    return cleaned;
                }
            }
        } catch (Exception ignored) {}
        return "poliester";
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
                "model", fastModel,
                "messages", List.of(Map.of(
                    "role", "user",
                    "content", prompt
                )),
                "stream", false
            ))
            .retrieve()
            .body(Map.class);

        return extractChatResponse(response);
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
