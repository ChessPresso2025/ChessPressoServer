package org.example.chesspressoserver.repository;

import org.example.chesspressoserver.models.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, UUID> {

    @Modifying
    @Transactional
    @Query("UPDATE UserStats s SET s.wins = s.wins + 1 WHERE s.userId = :userId")
    void incrementWins(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE UserStats s SET s.losses = s.losses + 1 WHERE s.userId = :userId")
    void incrementLosses(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE UserStats s SET s.draws = s.draws + 1 WHERE s.userId = :userId")
    void incrementDraws(UUID userId);
}
