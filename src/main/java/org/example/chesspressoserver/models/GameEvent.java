package org.example.chesspressoserver.models;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "game_events")
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(nullable = false)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Konstruktoren
    public GameEvent() {}

    public GameEvent(UUID userId, String type, Map<String, Object> payload) {
        this.userId = userId;
        this.type = type;
        this.payload = payload;
    }

    // Getter und Setter
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Builder-Pattern
    public static GameEventBuilder builder() {
        return new GameEventBuilder();
    }

    public static class GameEventBuilder {
        private UUID userId;
        private String type;
        private Map<String, Object> payload;

        public GameEventBuilder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public GameEventBuilder type(String type) {
            this.type = type;
            return this;
        }

        public GameEventBuilder payload(Map<String, Object> payload) {
            this.payload = payload;
            return this;
        }

        public GameEvent build() {
            return new GameEvent(userId, type, payload);
        }
    }
}
