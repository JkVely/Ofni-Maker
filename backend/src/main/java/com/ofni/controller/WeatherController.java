package com.ofni.controller;

import com.ofni.service.WeatherService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weather;

    public WeatherController(WeatherService weather) {
        this.weather = weather;
    }

    @GetMapping
    public WeatherService.WeatherResult get(@RequestParam double lat, @RequestParam double lon) {
        return weather.getCurrentTemperature(lat, lon);
    }
}
