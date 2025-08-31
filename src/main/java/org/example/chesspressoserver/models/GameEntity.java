package org.example.chesspressoserver.models;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "games")
public class GameEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "result")
    private String result;

    @Column(name = "lobby_id", nullable = false, unique = true)
    private String lobbyId;

    @Column(name = "white_player_id", nullable = false)
    private UUID whitePlayerId;

    @Column(name = "black_player_id", nullable = false)
    private UUID blackPlayerId;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MoveEntity> moves;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(OffsetDateTime endedAt) { this.endedAt = endedAt; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getLobbyId() { return lobbyId; }
    public void setLobbyId(String lobbyId) { this.lobbyId = lobbyId; }
    public UUID getWhitePlayerId() { return whitePlayerId; }
    public void setWhitePlayerId(UUID whitePlayerId) { this.whitePlayerId = whitePlayerId; }
    public UUID getBlackPlayerId() { return blackPlayerId; }
    public void setBlackPlayerId(UUID blackPlayerId) { this.blackPlayerId = blackPlayerId; }
    public List<MoveEntity> getMoves() { return moves; }
    public void setMoves(List<MoveEntity> moves) { this.moves = moves; }
}
