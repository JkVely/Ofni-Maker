package com.ofni.repository;

import com.ofni.model.OutfitEntity;
import com.ofni.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutfitRepository extends JpaRepository<OutfitEntity, Long> {

    List<OutfitEntity> findBySeason(Season season);

    List<OutfitEntity> findByGeneratedByAiTrue();

    List<OutfitEntity> findByGeneratedByAiFalse();

    @Query("SELECT o FROM OutfitEntity o WHERE o.minTemperature <= :temp AND o.maxTemperature >= :temp")
    List<OutfitEntity> findByTemperatureRange(@Param("temp") Double temperature);
}
