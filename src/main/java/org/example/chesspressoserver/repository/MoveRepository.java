package org.example.chesspressoserver.repository;

import org.example.chesspressoserver.models.MoveEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MoveRepository extends JpaRepository<MoveEntity, UUID> {
    List<MoveEntity> findByGameIdOrderByMoveNumberAsc(UUID gameId);
}
