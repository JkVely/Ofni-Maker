package com.ofni.repository;

import com.ofni.model.OutfitEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

@Repository
public interface OutfitRepository extends JpaRepository<OutfitEntity, Long> {

    List<OutfitEntity> findByUserId(Long userId);

    @Query("SELECT o FROM OutfitEntity o WHERE :temp BETWEEN o.minTemperature AND o.maxTemperature")
    List<OutfitEntity> findByTemperatureRange(@Param("temp") Double temp);

    @Query("SELECT o FROM OutfitEntity o WHERE o.userId = :userId AND :temp BETWEEN o.minTemperature AND o.maxTemperature")
    List<OutfitEntity> findByUserIdAndTemperatureRange(@Param("userId") Long userId, @Param("temp") Double temp);
}
