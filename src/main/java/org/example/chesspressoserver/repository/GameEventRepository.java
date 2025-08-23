package org.example.chesspressoserver.repository;

import org.example.chesspressoserver.models.GameEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GameEventRepository extends JpaRepository<GameEvent, UUID> {

    @Query("SELECT e FROM GameEvent e WHERE e.userId = :userId ORDER BY e.createdAt DESC")
    Page<GameEvent> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
