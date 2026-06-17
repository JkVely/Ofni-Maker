package com.ofni.dto;

import com.ofni.model.Category;
import com.ofni.model.Slot;
import java.time.LocalDateTime;
import java.util.List;

public record ClothResponse(
    Long id,
    String name,
    String description,
    String originalImagePath,
    String processedImagePath,
    Category category,
    Slot slot,
    String material,
    List<String> colorPalette,
    Integer warmthScore,
    Integer coverageScore,
    Boolean favorite,
    Boolean longSleeve,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
