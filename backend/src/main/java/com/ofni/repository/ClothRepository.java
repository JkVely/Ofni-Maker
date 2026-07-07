package com.ofni.repository;

import com.ofni.model.Category;
import com.ofni.model.ClothEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClothRepository extends JpaRepository<ClothEntity, Long> {

    List<ClothEntity> findByUserId(Long userId);

    List<ClothEntity> findByUserIdAndCategory(Long userId, Category category);

    List<ClothEntity> findByUserIdAndFavoriteTrue(Long userId);

    List<ClothEntity> findByUserIdAndWarmthScoreGreaterThanEqual(Long userId, Integer minWarmth);

    List<ClothEntity> findByUserIdAndWarmthScoreLessThanEqual(Long userId, Integer maxWarmth);
}
