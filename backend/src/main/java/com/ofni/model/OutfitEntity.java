package com.ofni.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
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
@Table(name = "outfits")
public class OutfitEntity extends BaseEntity {

    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private Season season;

    @Enumerated(EnumType.STRING)
    private Occasion occasion;

    private Double minTemperature;

    private Double maxTemperature;

    private String imagePath;

    private Boolean generatedByAi;

    @ManyToMany
    @JoinTable(
        name = "outfit_items",
        joinColumns = @JoinColumn(name = "outfit_id"),
        inverseJoinColumns = @JoinColumn(name = "cloth_id")
    )
    @OrderColumn(name = "item_order")
    private List<ClothEntity> items;
}
