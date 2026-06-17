package com.ofni.dto;

import com.ofni.model.Occasion;
import jakarta.validation.constraints.NotNull;

public record OutfitGenerateRequest(
    @NotNull
    Occasion occasion,

    Double latitude,

    Double longitude
) {}
