package org.example.chesspressoserver.controller;

import org.example.chesspressoserver.gamelogic.GameManager;
import org.example.chesspressoserver.models.requests.RematchRequest;
import org.example.chesspressoserver.models.requests.ResignGameRequest;
import org.example.chesspressoserver.models.requests.StartGameRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/game")
public class GameRestController {

    private final GameManager gameManager;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GameRestController(GameManager gameManager, SimpMessagingTemplate messagingTemplate) {
        this.gameManager = gameManager;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startGame(@RequestBody StartGameRequest request) {
        if (request.getLobbyId() == null || request.getLobbyId().isEmpty()) {
            return ResponseEntity.badRequest().body("Lobby-ID fehlt");
        }
        gameManager.startGame(request.getLobbyId());
        // WebSocket-Benachrichtigung an alle Clients der Lobby
        messagingTemplate.convertAndSend(
            "/topic/lobby/" + request.getLobbyId(),
            Map.of("type", "gameStarted", "lobbyId", request.getLobbyId())
        );
        return ResponseEntity.ok().body("Game started for Lobby-ID: " + request.getLobbyId());
    }

    @PostMapping("/resign")
    public ResponseEntity<?> resignGame(@RequestBody ResignGameRequest request) {
        if (request.getLobbyId() == null || request.getLobbyId().isEmpty()) {
            return ResponseEntity.badRequest().body("Lobby-ID fehlt");
        }
        boolean success = gameManager.resignGame(request.getLobbyId());
        if (success) {
            return ResponseEntity.ok().body("Spiel für Lobby aufgegeben");
        } else {
            return ResponseEntity.badRequest().body("Ungültige Lobby-ID");
        }
    }

    @PostMapping("/rematch")
    public ResponseEntity<?> requestRematch(@RequestBody RematchRequest request) {
        if (request.getLobbyId() == null || request.getLobbyId().isEmpty()) {
            return ResponseEntity.badRequest().body("Lobby-ID fehlt");
        }
        boolean success = gameManager.rematch(request.getLobbyId());
        if (success) {
            return ResponseEntity.ok().body("Rematch gestartet für Lobby-ID: " + request.getLobbyId());
        } else {
            return ResponseEntity.badRequest().body("Ungültige Lobby-ID für Rematch");
        }
    }
}
