package com.ofni.dto;

import com.ofni.model.Occasion;
import com.ofni.model.Season;
import java.time.LocalDateTime;
import java.util.List;

public record OutfitResponse(
    Long id,
    String name,
    String description,
    Season season,
    Occasion occasion,
    Double minTemperature,
    Double maxTemperature,
    String imagePath,
    Boolean generatedByAi,
    List<ClothResponse> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
