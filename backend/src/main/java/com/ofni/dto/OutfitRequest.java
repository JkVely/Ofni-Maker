package com.ofni.dto;

import com.ofni.model.Occasion;
import com.ofni.model.Season;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OutfitRequest(
    @NotBlank
    @Size(max = 100)
    String name,

    @Size(max = 500)
    String description,

    Season season,

    Occasion occasion,

    Double minTemperature,

    Double maxTemperature,

    List<Long> clothIds
) {}
