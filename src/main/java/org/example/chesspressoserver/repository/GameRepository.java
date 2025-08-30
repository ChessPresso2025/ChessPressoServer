package org.example.chesspressoserver.repository;

import org.example.chesspressoserver.models.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GameRepository extends JpaRepository<GameEntity, UUID> {
    @Query("SELECT g FROM GameEntity g WHERE g.userId = :userId ORDER BY g.startedAt DESC")
    List<GameEntity> findTop10ByUserIdOrderByStartedAtDesc(@Param("userId") UUID userId);
}

