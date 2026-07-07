package com.ofni.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ofni.dto.ClothRequest;
import com.ofni.dto.ClothResponse;
import com.ofni.exception.ResourceNotFoundException;
import com.ofni.model.Category;
import com.ofni.model.ClothEntity;
import com.ofni.model.Slot;
import com.ofni.repository.ClothRepository;

@Service
@Transactional
public class ClothService {

    private final ClothRepository repository;
    private final FashionpediaService fashionpedia;
    private final ColorExtractionService colors;
    private final OllamaClient ollama;
    private final BackgroundRemovalService backgroundRemoval;
    private final Path uploadDir;

    public ClothService(
        ClothRepository repository,
        FashionpediaService fashionpedia,
        ColorExtractionService colors,
        OllamaClient ollama,
        BackgroundRemovalService backgroundRemoval,
        @Value("${app.uploads.directory}") String uploadDir
    ) {
        this.repository = repository;
        this.fashionpedia = fashionpedia;
        this.colors = colors;
        this.ollama = ollama;
        this.backgroundRemoval = backgroundRemoval;
        this.uploadDir = Path.of(uploadDir);
    }

    public ClothResponse save(String originalFilename, byte[] imageBytes, Long userId) {
        try {
            var originalDir = uploadDir.resolve("original");
            var processedDir = uploadDir.resolve("processed");
            Files.createDirectories(originalDir);
            Files.createDirectories(processedDir);

            var safeFilename = sanitizeFilename(originalFilename);
            var uniqueName = UUID.randomUUID() + "_" + safeFilename;
            var originalPath = originalDir.resolve(uniqueName);
            var processedPath = processedDir.resolve(uniqueName);
            Files.write(originalPath, imageBytes);

            if (!backgroundRemoval.removeBackground(originalPath, processedPath)) {
                Files.copy(originalPath, processedPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            var fpResult = fashionpedia.classify(originalPath.toString());

            org.bytedeco.opencv.opencv_core.Rect cropBox = null;
            if (fpResult.confidence() >= 0.4f && fpResult.boxW() > 0 && fpResult.boxH() > 0) {
                int bx = Math.max(0, Math.round((fpResult.boxCx() - fpResult.boxW() / 2f) * fpResult.origW()));
                int by = Math.max(0, Math.round((fpResult.boxCy() - fpResult.boxH() / 2f) * fpResult.origH()));
                int bw = Math.min(fpResult.origW() - bx, Math.max(1, Math.round(fpResult.boxW() * fpResult.origW())));
                int bh = Math.min(fpResult.origH() - by, Math.max(1, Math.round(fpResult.boxH() * fpResult.origH())));
                cropBox = new org.bytedeco.opencv.opencv_core.Rect(bx, by, bw, bh);
            }

            var palette = colors.extractPalette(processedPath.toString(), cropBox);

            Category category;
            Slot slot;
            boolean longSleeve;
            String name;

            if (fpResult.confidence() >= 0.4f) {
                category = fpResult.category();
                slot = fpResult.slot();
                longSleeve = false;
                name = nombrePrenda(category);
            } else {
                try {
                    var ollamaCat = ollama.classifyItem(originalPath.toString());
                    category = Category.valueOf(ollamaCat.name());
                    slot = Slot.valueOf(ollamaCat.slot());
                    longSleeve = false;
                    name = nombrePrenda(category);
                } catch (Exception e) {
                    category = fpResult.category();
                    slot = fpResult.slot();
                    longSleeve = false;
                    name = nombrePrenda(category);
                }
            }

            var entity = ClothEntity.builder()
                .name(name)
                .description("Un/una %s.".formatted(name.toLowerCase()))
                .originalImagePath(originalPath.toString())
                .processedImagePath(processedPath.toString())
                .category(category)
                .slot(slot)
                .colorPalette(palette)
                .warmthScore(WarmthCalculator.warmthScore(category, null, longSleeve))
                .coverageScore(WarmthCalculator.coverageScore(category))
                .longSleeve(longSleeve)
                .favorite(false)
                .userId(userId)
                .build();

            entity = repository.save(entity);

            analyzeMaterialAsync(entity.getId(), processedPath.toString(), category);

            return toResponse(entity);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to save image", e);
        }
    }

    private static String sanitizeFilename(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[\\.\\./\\\\]", "_")
            .replaceAll("[^a-zA-Z0-9._-]", "_")
            .replaceAll("_{2,}", "_")
            .replaceAll("^_|_$", "");
    }

    private String nombrePrenda(Category cat) {
        return switch (cat) {
            case TSHIRT -> "Remera";
            case SHIRT -> "Camisa";
            case POLO -> "Polo";
            case BLOUSE -> "Blusa";
            case SWEATER -> "Sueter";
            case HOODIE -> "Buzo";
            case JACKET -> "Chaqueta";
            case COAT -> "Abrigo";
            case PANTS -> "Pantalon";
            case JEANS -> "Jeans";
            case SHORTS -> "Short";
            case SKIRT -> "Falda";
            case DRESS -> "Vestido";
            case SHOES -> "Zapatos";
            case SNEAKERS -> "Zapatillas";
            case BOOTS -> "Botas";
            case SANDALS -> "Sandalias";
            case HAT -> "Gorro";
            case SCARF -> "Bufanda";
            case BELT -> "Cinturon";
            case BAG -> "Bolso";
            case ACCESSORY -> "Accesorio";
            case OTHER -> "Prenda";
        };
    }

    @Async
    void analyzeMaterialAsync(Long clothId, String imagePath, Category detectedCategory) {
        repository.findById(clothId).ifPresent(entity -> {
            try {
                var material = ollama.analyzeMaterial(imagePath, detectedCategory);
                if (material == null || material.isBlank() || material.length() > 20
                    || material.equalsIgnoreCase("poliester")) {
                    material = "poliester";
                }
                entity.setMaterial(material);
                entity.setWarmthScore(WarmthCalculator.warmthScore(
                    entity.getCategory(), material, entity.getLongSleeve()));
                entity.setDescription("Un/una %s de %s.".formatted(
                    nombrePrenda(entity.getCategory()).toLowerCase(), material));
                repository.save(entity);
            } catch (Exception e) {
                entity.setMaterial("poliester");
                repository.save(entity);
            }
        });
    }

    public ClothResponse update(Long id, ClothRequest request, Long userId) {
        var entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cloth", id));
        if (!entity.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Cloth", id);
        }

        if (request.name() != null)           entity.setName(request.name());
        if (request.description() != null)    entity.setDescription(request.description());
        if (request.category() != null)       entity.setCategory(request.category());
        if (request.slot() != null)           entity.setSlot(request.slot());
        if (request.material() != null)       entity.setMaterial(request.material());
        if (request.colorPalette() != null)   entity.setColorPalette(request.colorPalette());
        if (request.favorite() != null)       entity.setFavorite(request.favorite());
        if (request.longSleeve() != null)     entity.setLongSleeve(request.longSleeve());

        if (request.warmthScore() != null) {
            entity.setWarmthScore(request.warmthScore());
        } else if (request.material() != null || request.longSleeve() != null) {
            entity.setWarmthScore(WarmthCalculator.warmthScore(
                entity.getCategory(),
                request.material() != null ? request.material() : entity.getMaterial(),
                request.longSleeve() != null ? request.longSleeve() : entity.getLongSleeve()
            ));
        }

        if (request.coverageScore() != null) {
            entity.setCoverageScore(request.coverageScore());
        }

        return toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<ClothResponse> findAllByUser(Long userId) {
        return repository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClothResponse findById(Long id, Long userId) {
        var entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cloth", id));
        if (!entity.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Cloth", id);
        }
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ClothResponse> findBySlot(String slot, Long userId) {
        return repository.findByUserId(userId).stream()
            .filter(c -> c.getSlot().name().equalsIgnoreCase(slot))
            .map(this::toResponse)
            .toList();
    }

    public void delete(Long id, Long userId) {
        var entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cloth", id));
        if (!entity.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Cloth", id);
        }
        repository.delete(entity);
    }

    private ClothResponse toResponse(ClothEntity e) {
        return new ClothResponse(
            e.getId(), e.getName(), e.getDescription(),
            e.getOriginalImagePath(), e.getProcessedImagePath(),
            e.getCategory(), e.getSlot(), e.getMaterial(),
            e.getColorPalette(), e.getWarmthScore(), e.getCoverageScore(),
            e.getFavorite(), e.getLongSleeve(),
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
