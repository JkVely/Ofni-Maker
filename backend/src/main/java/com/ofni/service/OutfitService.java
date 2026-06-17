package com.ofni.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ofni.dto.ClothResponse;
import com.ofni.dto.OutfitGenerateRequest;
import com.ofni.dto.OutfitRequest;
import com.ofni.dto.OutfitResponse;
import com.ofni.exception.InvalidOutfitException;
import com.ofni.exception.ResourceNotFoundException;
import com.ofni.model.ClothEntity;
import com.ofni.model.OutfitEntity;
import com.ofni.repository.ClothRepository;
import com.ofni.repository.OutfitRepository;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class OutfitService {

    private final OutfitRepository outfitRepository;
    private final ClothRepository clothRepository;
    private final WeatherService weatherService;
    private final OllamaClient ollama;
    private final ObjectMapper mapper;

    public OutfitService(
        OutfitRepository outfitRepository,
        ClothRepository clothRepository,
        WeatherService weatherService,
        OllamaClient ollama
    ) {
        this.outfitRepository = outfitRepository;
        this.clothRepository = clothRepository;
        this.weatherService = weatherService;
        this.ollama = ollama;
        this.mapper = new ObjectMapper();
    }

    public OutfitResponse create(OutfitRequest request) {
        var items = clothRepository.findAllById(request.clothIds());
        validateOutfit(items);

        var outfit = OutfitEntity.builder()
            .name(request.name())
            .description(request.description())
            .season(request.season())
            .occasion(request.occasion())
            .minTemperature(request.minTemperature())
            .maxTemperature(request.maxTemperature())
            .items(items)
            .generatedByAi(false)
            .build();

        return toResponse(outfitRepository.save(outfit));
    }

    public OutfitResponse generate(OutfitGenerateRequest request) {
        var weather = (request.latitude() != null && request.longitude() != null)
            ? weatherService.getCurrentTemperature(request.latitude(), request.longitude())
            : new WeatherService.WeatherResult(20.0, "°C");

        var candidates = clothRepository.findByWarmthScoreGreaterThanEqual(
            warmthFloor(weather.temperature()));

        var candidateResponses = candidates.stream()
            .map(this::toClothResponse)
            .toList();

        var aiResponse = ollama.generateOutfit(
            candidateResponses, request.occasion(), weather.temperature());

        var selectedIds = parseSelectedIds(aiResponse);
        var items = clothRepository.findAllById(selectedIds);
        validateOutfit(items);

        var outfit = OutfitEntity.builder()
            .name(parseName(aiResponse))
            .occasion(request.occasion())
            .minTemperature(weather.temperature() - 5)
            .maxTemperature(weather.temperature() + 5)
            .items(items)
            .generatedByAi(true)
            .build();

        return toResponse(outfitRepository.save(outfit));
    }

    @Transactional(readOnly = true)
    public OutfitResponse findById(Long id) {
        return outfitRepository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Outfit", id));
    }

    @Transactional(readOnly = true)
    public List<OutfitResponse> findAll() {
        return outfitRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OutfitResponse> findByTemperature(Double temp) {
        return outfitRepository.findByTemperatureRange(temp).stream()
            .map(this::toResponse)
            .toList();
    }

    public void delete(Long id) {
        var outfit = outfitRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Outfit", id));
        outfitRepository.delete(outfit);
    }

    private void validateOutfit(List<ClothEntity> items) {
        if (items.size() < 3) {
            throw new InvalidOutfitException(
                "Outfit must have at least 3 items (top, bottom, footwear)");
        }

        var slots = items.stream().map(ClothEntity::getSlot).toList();
        var hasTop = slots.stream().anyMatch(s -> s.name().equals("TOP"));
        var hasFootwear = slots.stream().anyMatch(s -> s.name().equals("FOOTWEAR"));
        var hasBottom = slots.stream().anyMatch(s -> s.name().equals("BOTTOM"));
        var hasDress = items.stream().anyMatch(c -> c.getCategory().name().equals("DRESS"));

        if (!hasTop) {
            throw new InvalidOutfitException("Outfit must include a TOP item");
        }
        if (!hasFootwear) {
            throw new InvalidOutfitException("Outfit must include FOOTWEAR");
        }
        if (!hasBottom && !hasDress) {
            throw new InvalidOutfitException("Outfit must include BOTTOM or a DRESS in TOP slot");
        }
    }

    private static int warmthFloor(double temperature) {
        if (temperature >= 25) return 1;
        if (temperature >= 15) return 2;
        if (temperature >= 10) return 3;
        return 4;
    }

    private List<Long> parseSelectedIds(String aiResponse) {
        var json = extractJson(aiResponse);
        if (json == null) return new ArrayList<>();
        try {
            Map<String, Object> map = mapper.readValue(json, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            var ids = (List<Integer>) map.get("selected_ids");
            if (ids == null) return new ArrayList<>();
            return ids.stream().map(Long::valueOf).toList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String parseName(String aiResponse) {
        var json = extractJson(aiResponse);
        if (json == null) return "Outfit IA";
        try {
            Map<String, Object> map = mapper.readValue(json, new TypeReference<>() {});
            var name = (String) map.get("name");
            return name != null ? name : "Outfit IA";
        } catch (Exception e) {
            return "Outfit IA";
        }
    }

    private static String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) return null;
        return text.substring(start, end + 1);
    }

    private ClothResponse toClothResponse(ClothEntity e) {
        return new ClothResponse(
            e.getId(), e.getName(), e.getDescription(),
            e.getOriginalImagePath(), e.getProcessedImagePath(),
            e.getCategory(), e.getSlot(), e.getMaterial(),
            e.getColorPalette(), e.getWarmthScore(), e.getCoverageScore(),
            e.getFavorite(), e.getLongSleeve(),
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private OutfitResponse toResponse(OutfitEntity e) {
        return new OutfitResponse(
            e.getId(), e.getName(), e.getDescription(),
            e.getSeason(), e.getOccasion(),
            e.getMinTemperature(), e.getMaxTemperature(),
            e.getImagePath(), e.getGeneratedByAi(),
            e.getItems().stream().map(this::toClothResponse).toList(),
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
