package com.ofni.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Service
public class WeatherService {

    private final RestClient client;

    public WeatherService() {
        this.client = RestClient.create("https://api.open-meteo.com");
    }

    public record WeatherResult(double temperature, String unit) {}

    /** Obtiene la temperatura actual para una ubicacion (lat, lon). */
    public WeatherResult getCurrentTemperature(double latitude, double longitude) {
        var response = client.get()
            .uri(uri -> uri
                .path("/v1/forecast")
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current_weather", "true")
                .build())
            .retrieve()
            .body(Map.class);

        if (response == null) {
            return new WeatherResult(20.0, "°C"); // fallback
        }

        @SuppressWarnings("unchecked")
        var current = (Map<String, Object>) response.get("current_weather");
        if (current == null) {
            return new WeatherResult(20.0, "°C");
        }

        var temp = ((Number) current.getOrDefault("temperature", 20.0)).doubleValue();
        return new WeatherResult(temp, "°C");
    }
}
