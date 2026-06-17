package com.ofni.repository;

import com.ofni.model.Category;
import com.ofni.model.ClothEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClothRepository extends JpaRepository<ClothEntity, Long> {

    List<ClothEntity> findByCategory(Category category);

    List<ClothEntity> findByFavoriteTrue();

    List<ClothEntity> findByWarmthScoreGreaterThanEqual(Integer minWarmth);

    List<ClothEntity> findByWarmthScoreLessThanEqual(Integer maxWarmth);
}
