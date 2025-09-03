package org.example.chesspressoserver.controller;

import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.models.Lobby;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/lobby")
public class LobbyApiController {

    private final LobbyService lobbyService;

    public LobbyApiController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    private static final String LOBBY_ID_KEY = "lobbyId";
    private static final String SUCCESS_KEY = "success";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String ERROR_KEY = "error";


     //Aufruf: http://localhost:8080/api/lobby/all

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllLobbies() {
        try {
            Collection<Lobby> allLobbies = lobbyService.getAllActiveLobbies();

            // Konvertiere Lobbies zu einer übersichtlichen Liste für Browser-Anzeige
            List<Map<String, Object>> lobbyList = allLobbies.stream()
                .map(lobby -> {
                    Map<String, Object> lobbyMap = new HashMap<>();
                    lobbyMap.put(LOBBY_ID_KEY, lobby.getLobbyId());
                    lobbyMap.put("lobbyType", lobby.getLobbyType().toString());
                    lobbyMap.put("status", lobby.getStatus().toString());
                    lobbyMap.put("playerCount", lobby.getPlayers().size());
                    lobbyMap.put("maxPlayers", 2);
                    lobbyMap.put("gameTime", lobby.getGameTime() != null ? lobby.getGameTime().toString() : "Nicht gesetzt");
                    lobbyMap.put("isGameStarted", lobby.isGameStarted());
                    lobbyMap.put("creator", lobby.getCreator());
                    lobbyMap.put("createdAt", lobby.getCreatedAt() != null ? lobby.getCreatedAt().toString() : "Unbekannt");
                    return lobbyMap;
                })
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put(SUCCESS_KEY, true);
            response.put("totalLobbies", lobbyList.size());
            response.put("lobbies", lobbyList);
            response.put(TIMESTAMP_KEY, java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(SUCCESS_KEY, false);
            errorResponse.put(ERROR_KEY, "Fehler beim Abrufen der Lobbies: " + e.getMessage());
            errorResponse.put(TIMESTAMP_KEY, java.time.LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }


     //http://localhost:8080/api/lobby/{lobbyId}

    @GetMapping("/{lobbyId}")
    public ResponseEntity<Map<String, Object>> getLobbyDetails(@PathVariable String lobbyId) {
        try {
            Lobby lobby = lobbyService.getLobby(lobbyId);

            if (lobby == null) {
                return ResponseEntity.notFound().build();
            }

            // Detaillierte Lobby-Informationen für Browser-Anzeige
            Map<String, Object> lobbyDetails = new HashMap<>();
            lobbyDetails.put(LOBBY_ID_KEY, lobby.getLobbyId());
            lobbyDetails.put("lobbyType", lobby.getLobbyType().toString());
            lobbyDetails.put("status", lobby.getStatus().toString());
            lobbyDetails.put("players", lobby.getPlayers());
            lobbyDetails.put("playerCount", lobby.getPlayers().size());
            lobbyDetails.put("maxPlayers", 2);
            lobbyDetails.put("gameTime", lobby.getGameTime() != null ? lobby.getGameTime().toString() : "Nicht gesetzt");
            lobbyDetails.put("isGameStarted", lobby.isGameStarted());
            lobbyDetails.put("creator", lobby.getCreator());
            lobbyDetails.put("createdAt", lobby.getCreatedAt() != null ? lobby.getCreatedAt().toString() : "Unbekannt");
            lobbyDetails.put("isFull", lobby.getPlayers().size() >= 2);

            Map<String, Object> response = new HashMap<>();
            response.put(SUCCESS_KEY, true);
            response.put("lobby", lobbyDetails);
            response.put(TIMESTAMP_KEY, java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(SUCCESS_KEY, false);
            errorResponse.put(ERROR_KEY, "Fehler beim Abrufen der Lobby-Details: " + e.getMessage());
            errorResponse.put(TIMESTAMP_KEY, java.time.LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    //http://localhost:8080/api/lobby/stats

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getLobbyStatistics() {
        try {
            Collection<Lobby> allLobbies = lobbyService.getAllActiveLobbies();

            long publicLobbies = allLobbies.stream()
                .filter(lobby -> lobby.getLobbyType().toString().equals("PUBLIC"))
                .count();

            long privateLobbies = allLobbies.stream()
                .filter(lobby -> lobby.getLobbyType().toString().equals("PRIVATE"))
                .count();

            long waitingLobbies = allLobbies.stream()
                .filter(lobby -> lobby.getStatus().toString().equals("WAITING"))
                .count();

            long fullLobbies = allLobbies.stream()
                .filter(lobby -> lobby.getStatus().toString().equals("FULL"))
                .count();

            long gameStartedLobbies = allLobbies.stream()
                .filter(Lobby::isGameStarted)
                .count();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalLobbies", allLobbies.size());
            statistics.put("publicLobbies", publicLobbies);
            statistics.put("privateLobbies", privateLobbies);
            statistics.put("waitingLobbies", waitingLobbies);
            statistics.put("fullLobbies", fullLobbies);
            statistics.put("activeGames", gameStartedLobbies);

            Map<String, Object> response = new HashMap<>();
            response.put(SUCCESS_KEY, true);
            response.put("statistics", statistics);
            response.put(TIMESTAMP_KEY, java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(SUCCESS_KEY, false);
            errorResponse.put(ERROR_KEY, "Fehler beim Abrufen der Statistiken: " + e.getMessage());
            errorResponse.put(TIMESTAMP_KEY, java.time.LocalDateTime.now());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // http://localhost:8080/api/lobby/leave

    @PostMapping("/leave")
    public ResponseEntity<Map<String, Object>> leaveLobby(@RequestBody Map<String, String> request, Principal principal) {
        try {
            String lobbyId = request.get(LOBBY_ID_KEY);
            String playerId = principal.getName();

            if (lobbyId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    SUCCESS_KEY, false,
                    ERROR_KEY, "LobbyId muss angegeben werden"
                ));
            }

            lobbyService.leaveLobby(playerId, lobbyId);

            return ResponseEntity.ok(Map.of(
                SUCCESS_KEY, true,
                "message", "Lobby erfolgreich verlassen"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                SUCCESS_KEY, false,
                ERROR_KEY, "Fehler beim Verlassen der Lobby: " + e.getMessage()
            ));
        }
    }
}