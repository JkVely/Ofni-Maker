package com.ofni.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "cloths")
public class ClothEntity extends BaseEntity {

    private String name;

    private String description;

    private String originalImagePath;

    private String processedImagePath;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    private Slot slot;

    private String material;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cloth_colors", joinColumns = @JoinColumn(name = "cloth_id"))
    @Column(name = "hex_color")
    private List<String> colorPalette;

    private Integer warmthScore;

    private Integer coverageScore;

    private Boolean favorite;

    private Boolean longSleeve;
}
