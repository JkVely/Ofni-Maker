package com.ofni.service;

import com.ofni.model.Category;
import com.ofni.model.Slot;

final class FashionpediaMapper {

    private static final String[] LABELS = {
        "shirt, blouse",                         // 0
        "top, t-shirt, sweatshirt",              // 1
        "sweater",                               // 2
        "cardigan",                              // 3
        "jacket",                                // 4
        "vest",                                  // 5
        "pants",                                 // 6
        "shorts",                                // 7
        "skirt",                                 // 8
        "coat",                                  // 9
        "dress",                                 // 10
        "jumpsuit",                              // 11
        "cape",                                  // 12
        "glasses",                               // 13
        "hat",                                   // 14
        "headband, head covering, hair accessory", // 15
        "tie",                                   // 16
        "glove",                                 // 17
        "watch",                                 // 18
        "belt",                                  // 19
        "leg warmer",                           // 20
        "tights, stockings",                    // 21
        "sock",                                 // 22
        "shoe",                                 // 23
        "bag, wallet",                          // 24
        "scarf",                                // 25
        "umbrella",                             // 26
        // garment parts below — mapped to OTHER/ACCESSORY
        "hood",                                 // 27
        "collar",                               // 28
        "lapel",                                // 29
        "epaulette",                            // 30
        "sleeve",                               // 31
        "pocket",                               // 32
        "neckline",                             // 33
        "buckle",                               // 34
        "zipper",                               // 35
        "applique",                             // 36
        "bead",                                 // 37
        "bow",                                  // 38
        "flower",                               // 39
        "fringe",                               // 40
        "ribbon",                               // 41
        "rivet",                                // 42
        "ruffle",                               // 43
        "sequin",                               // 44
        "tassel",                               // 45
    };

    static String labelAt(int index) {
        return (index >= 0 && index < LABELS.length) ? LABELS[index] : "unknown";
    }

    static Category toCategory(int fpIndex) {
        return switch (fpIndex) {
            case 0 -> Category.SHIRT;
            case 1 -> Category.TSHIRT;
            case 2 -> Category.SWEATER;
            case 3 -> Category.SWEATER;    // cardigan -> SWEATER
            case 4 -> Category.JACKET;
            case 5 -> Category.TSHIRT;     // vest -> TSHIRT (sleeveless top)
            case 6 -> Category.PANTS;
            case 7 -> Category.SHORTS;
            case 8 -> Category.SKIRT;
            case 9 -> Category.COAT;
            case 10 -> Category.DRESS;
            case 11 -> Category.DRESS;     // jumpsuit -> DRESS
            case 12 -> Category.OTHER;     // cape -> OTHER
            case 13 -> Category.ACCESSORY; // glasses
            case 14 -> Category.HAT;
            case 15 -> Category.ACCESSORY; // headband
            case 16 -> Category.ACCESSORY; // tie
            case 17 -> Category.ACCESSORY; // glove
            case 18 -> Category.ACCESSORY; // watch
            case 19 -> Category.BELT;
            case 20 -> Category.ACCESSORY; // leg warmer
            case 21 -> Category.ACCESSORY; // tights
            case 22 -> Category.ACCESSORY; // sock
            case 23 -> Category.SHOES;
            case 24 -> Category.BAG;
            case 25 -> Category.SCARF;
            case 26 -> Category.ACCESSORY; // umbrella
            default -> Category.OTHER;     // garment parts
        };
    }

    static Slot toSlot(Category category) {
        return switch (category) {
            case TSHIRT, SHIRT, POLO, BLOUSE, SWEATER, HOODIE, DRESS -> Slot.TOP;
            case JACKET, COAT -> Slot.OUTERWEAR;
            case PANTS, JEANS, SHORTS, SKIRT -> Slot.BOTTOM;
            case SHOES, SNEAKERS, BOOTS, SANDALS -> Slot.FOOTWEAR;
            case HAT -> Slot.HEAD;
            case SCARF, BELT, BAG, ACCESSORY, OTHER -> Slot.ACCESSORY;
        };
    }
}
