package com.ofni.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    private String imagePath;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String material;

    @ElementCollection
    private List<String> colorPalette;

    private Integer warmthScore;

    private Integer coverageScore;

    private Boolean favorite;
}
