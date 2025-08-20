package org.example.chesspressoserver.models;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "user_stats")
public class UserStats {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer wins = 0;

    @Column(nullable = false)
    private Integer losses = 0;

    @Column(nullable = false)
    private Integer draws = 0;

    // Konstruktoren
    public UserStats() {}

    public UserStats(UUID userId, User user, Integer wins, Integer losses, Integer draws) {
        this.userId = userId;
        this.user = user;
        this.wins = wins != null ? wins : 0;
        this.losses = losses != null ? losses : 0;
        this.draws = draws != null ? draws : 0;
    }

    // Getter und Setter
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getWins() { return wins; }
    public void setWins(Integer wins) { this.wins = wins; }

    public Integer getLosses() { return losses; }
    public void setLosses(Integer losses) { this.losses = losses; }

    public Integer getDraws() { return draws; }
    public void setDraws(Integer draws) { this.draws = draws; }

    // Builder-Pattern
    public static UserStatsBuilder builder() {
        return new UserStatsBuilder();
    }

    public static class UserStatsBuilder {
        private UUID userId;
        private User user;
        private Integer wins = 0;
        private Integer losses = 0;
        private Integer draws = 0;

        public UserStatsBuilder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public UserStatsBuilder user(User user) {
            this.user = user;
            return this;
        }

        public UserStatsBuilder wins(Integer wins) {
            this.wins = wins;
            return this;
        }

        public UserStatsBuilder losses(Integer losses) {
            this.losses = losses;
            return this;
        }

        public UserStatsBuilder draws(Integer draws) {
            this.draws = draws;
            return this;
        }

        public UserStats build() {
            return new UserStats(userId, user, wins, losses, draws);
        }
    }
}
