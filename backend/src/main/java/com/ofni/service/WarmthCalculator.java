package com.ofni.service;

import com.ofni.model.Category;
import com.ofni.model.Slot;
import com.ofni.util.DeepFashion2Mapper;

/**
 * Calcula warmthScore (1-5) y coverageScore (1-5) usando reglas deterministas.
 * No involucra IA — son heuristicas basadas en categoria, material y manga.
 */
public final class WarmthCalculator {

    private WarmthCalculator() {}

    public static int warmthScore(Category category, String material, boolean longSleeve) {
        var base = switch (category) {
            case TSHIRT, POLO, SHORTS, SKIRT, SANDALS             -> 1;
            case SHIRT, BLOUSE, DRESS, SNEAKERS, SHOES             -> 2;
            case SWEATER, HOODIE, PANTS, JEANS                     -> 3;
            case JACKET, BOOTS                                     -> 4;
            case COAT, HAT, SCARF                                  -> 5;
            case null, default                                     -> 2;
        };

        var mat = material != null ? material.toLowerCase() : "";
        var materialBonus = switch (mat) {
            case "wool", "lana", "cashmere", "cachemira",
                 "fleece", "polar", "down", "plumon"              -> 2;
            case "cotton", "algodon", "denim", "mezclilla",
                 "leather", "cuero", "acrylic", "acrilico",
                 "polyester", "poliester"                          -> 1;
            case "linen", "lino", "silk", "seda",
                 "viscose", "viscosa", "rayon"                     -> -1;
            default                                                -> 0;
        };

        var sleeveBonus = DeepFashion2Mapper.toSlot(category) == Slot.TOP && longSleeve ? 1 : 0;

        return clamp(base + materialBonus + sleeveBonus, 1, 5);
    }

    public static int coverageScore(Category category) {
        return switch (category) {
            case TSHIRT, POLO, SHORTS, SKIRT                       -> 2;
            case SHIRT, BLOUSE, DRESS, PANTS, JEANS                -> 3;
            case SWEATER, HOODIE, JACKET, COAT                     -> 4;
            default                                                -> 1;
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
