package com.ofni.util;

import com.ofni.model.Category;
import com.ofni.model.Slot;

/**
 * DeepFashion2 has 13 clothing categories (indices 0-12).
 * This mapper converts the raw ONNX output index into our domain enums.
 */
public final class DeepFashion2Mapper {

    private static final String[] DF2_LABELS = {
        "short sleeve top",    // 0
        "long sleeve top",     // 1
        "short sleeve outwear",// 2
        "long sleeve outwear", // 3
        "vest",                // 4
        "sling",               // 5
        "shorts",              // 6
        "trousers",            // 7
        "skirt",               // 8
        "short sleeve dress",  // 9
        "long sleeve dress",   // 10
        "vest dress",          // 11
        "sling dress"          // 12
    };

    public static String labelAt(int index) {
        return (index >= 0 && index < DF2_LABELS.length) ? DF2_LABELS[index] : "unknown";
    }

    public static Category toCategory(int df2Index) {
        return switch (df2Index) {
            case 0, 4, 5 -> Category.TSHIRT;
            case 1       -> Category.SHIRT;
            case 2       -> Category.JACKET;
            case 3       -> Category.COAT;
            case 6       -> Category.SHORTS;
            case 7       -> Category.PANTS;
            case 8       -> Category.SKIRT;
            case 9, 10, 11, 12 -> Category.DRESS;
            default      -> Category.OTHER;
        };
    }

    public static Slot toSlot(Category category) {
        return switch (category) {
            case TSHIRT, SHIRT, POLO, BLOUSE, SWEATER, HOODIE, DRESS -> Slot.TOP;
            case JACKET, COAT -> Slot.OUTERWEAR;
            case PANTS, JEANS, SHORTS, SKIRT -> Slot.BOTTOM;
            case SHOES, SNEAKERS, BOOTS, SANDALS -> Slot.FOOTWEAR;
            case HAT -> Slot.HEAD;
            case SCARF, BELT, BAG, ACCESSORY, OTHER -> Slot.ACCESSORY;
        };
    }

    public static boolean hasSleeves(int df2Index) {
        return switch (df2Index) {
            case 0, 2, 9  -> true;  // short sleeve variants
            case 1, 3, 10 -> true;  // long sleeve variants
            default       -> false; // vest, sling, shorts, trousers, etc.
        };
    }

    public static boolean isLongSleeve(int df2Index) {
        return switch (df2Index) {
            case 1, 3, 10 -> true;  // long sleeve variants
            default       -> false;
        };
    }
}
