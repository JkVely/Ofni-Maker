package com.ofni.dto;

import com.ofni.model.Category;
import com.ofni.model.Slot;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClothRequest(
    @Size(max = 100)
    String name,

    @Size(max = 500)
    String description,

    Category category,

    Slot slot,

    @Size(max = 50)
    String material,

    List<String> colorPalette,

    Integer warmthScore,

    Integer coverageScore,

    Boolean favorite,

    Boolean longSleeve
) {}
