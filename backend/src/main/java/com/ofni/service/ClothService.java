package com.ofni.service;

import com.ofni.dto.ClothRequest;
import com.ofni.dto.ClothResponse;
import com.ofni.exception.ResourceNotFoundException;
import com.ofni.model.ClothEntity;
import com.ofni.repository.ClothRepository;
import com.ofni.util.DeepFashion2Mapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClothService {

    private final ClothRepository repository;
    private final OnnxClassificationService onnx;
    private final ColorExtractionService colors;
    private final OllamaClient ollama;
    private final Path uploadDir;

    public ClothService(
        ClothRepository repository,
        OnnxClassificationService onnx,
        ColorExtractionService colors,
        OllamaClient ollama,
        @Value("${app.uploads.directory}") String uploadDir
    ) {
        this.repository = repository;
        this.onnx = onnx;
        this.colors = colors;
        this.ollama = ollama;
        this.uploadDir = Path.of(uploadDir);
    }

    public ClothResponse save(String originalFilename, byte[] imageBytes) {
        try {
            var originalPath = uploadDir.resolve("original");
            var processedPath = uploadDir.resolve("processed");
            Files.createDirectories(originalPath);
            Files.createDirectories(processedPath);

            var uniqueName = UUID.randomUUID() + "_" + originalFilename;
            var fullPath = originalPath.resolve(uniqueName);
            Files.write(fullPath, imageBytes);

            var result = onnx.classify(fullPath.toString());
            var category = DeepFashion2Mapper.toCategory(result.predictedIndex());
            var slot = DeepFashion2Mapper.toSlot(category);
            var longSleeve = DeepFashion2Mapper.isLongSleeve(result.predictedIndex());
            var palette = colors.extractPalette(fullPath.toString());

            var entity = ClothEntity.builder()
                .name(category.name())
                .originalImagePath(fullPath.toString())
                .processedImagePath(fullPath.toString())
                .category(category)
                .slot(slot)
                .colorPalette(palette)
                .warmthScore(WarmthCalculator.warmthScore(category, null, longSleeve))
                .coverageScore(WarmthCalculator.coverageScore(category))
                .longSleeve(longSleeve)
                .favorite(false)
                .build();

            entity = repository.save(entity);

            analyzeMaterialAsync(entity.getId(), fullPath.toString());

            return toResponse(entity);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to save image", e);
        }
    }

    @Async
    void analyzeMaterialAsync(Long clothId, String imagePath) {
        repository.findById(clothId).ifPresent(entity -> {
            try {
                var material = ollama.analyzeMaterial(imagePath);
                entity.setMaterial(material);
                entity.setWarmthScore(WarmthCalculator.warmthScore(
                    entity.getCategory(), material, entity.getLongSleeve()));
                repository.save(entity);
            } catch (Exception e) {
                entity.setMaterial("poliester");
                repository.save(entity);
            }
        });
    }

    public ClothResponse update(Long id, ClothRequest request) {
        var entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cloth", id));

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
    public ClothResponse findById(Long id) {
        return repository.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Cloth", id));
    }

    @Transactional(readOnly = true)
    public List<ClothResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ClothResponse> findBySlot(String slot) {
        return repository.findAll().stream()
            .filter(c -> c.getSlot().name().equalsIgnoreCase(slot))
            .map(this::toResponse)
            .toList();
    }

    public void delete(Long id) {
        var entity = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cloth", id));
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
