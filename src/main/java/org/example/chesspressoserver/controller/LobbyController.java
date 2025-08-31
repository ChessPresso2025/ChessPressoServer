package org.example.chesspressoserver.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chesspressoserver.models.GameTime;
import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/lobby")
public class LobbyController {

    private final LobbyService lobbyService;
    private final UserService userService;
    private final GameRestController gameRestController;

    public LobbyController(LobbyService lobbyService, UserService userService, GameRestController gameRestController) {
        this.lobbyService = lobbyService;
        this.userService = userService;
        this.gameRestController = gameRestController;
        // Setze GameRestController im LobbyService für automatischen Spielstart
        this.lobbyService.setGameRestController(gameRestController);
    }


    @PostMapping("/quick-join")
    public ResponseEntity<?> quickJoin(@RequestBody QuickJoinRequest request, Principal principal, HttpServletRequest httpRequest) {
        // Verwende Session-ID als eindeutige Spieler-ID falls keine vorhanden
        String playerId = principal != null ? principal.getName() :
                         httpRequest.getSession().getId();

        System.out.println("DEBUG: Quick join request from player: " + playerId);

        // Prüfe ob Spieler bereits in einer Lobby ist
        String existingLobby = lobbyService.getPlayerLobby(playerId);
        if (existingLobby != null) {
            // Prüfe ob die existierende Lobby noch aktiv und nicht im Spiel ist
            var lobby = lobbyService.getLobby(existingLobby);
            if (lobby != null && !lobby.isGameStarted()) {
                // Spieler ist in einer wartenden Lobby - Frontend-konforme Error Response
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Du bist bereits in einer Lobby",
                    "message", "Verlasse zuerst deine aktuelle Lobby",
                    "lobbyId", existingLobby
                ));
            } else {
                // Lobby existiert nicht mehr oder Spiel ist beendet - räume auf
                System.out.println("DEBUG: Cleaning up stale lobby reference for player: " + playerId);
                lobbyService.forceLeaveAllLobbies(playerId);
            }
        }

        try {
            String lobbyId = lobbyService.joinQuickMatch(playerId, request.getGameTime());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "lobbyId", lobbyId,
                "message", "Quick Match gestartet",
                "gameTime", request.getGameTime().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Fehler beim Quick Match: " + e.getMessage(),
                "message", "Bitte versuche es erneut"
            ));
        }
    }


    /**
     * Private Lobby erstellen
     */
    @PostMapping("/private/create")
    public ResponseEntity<?> createPrivateLobby(Principal principal, HttpServletRequest request) {
        // Verwende Session-ID als eindeutige Spieler-ID falls kein Principal vorhanden
        String playerId = principal != null ? principal.getName() :
                         request.getSession().getId();

        String playerName = userService.getUsernameById(playerId);

        System.out.println("DEBUG: Create lobby request from player: " + playerName);

        // Prüfe ob Spieler bereits in einer Lobby ist
        String existingLobby = lobbyService.getPlayerLobby(playerId);
        if (existingLobby != null) {
            System.out.println("DEBUG: Player already in lobby: " + existingLobby + ", cleaning up");
            lobbyService.forceLeaveAllLobbies(playerId);
        }

        try {
            String lobbyCode = lobbyService.createPrivateLobby(playerId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "lobbyCode", lobbyCode,
                "message", "Private Lobby erstellt",
                "playerId", playerName  // Für Debug
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Fehler beim Erstellen der Lobby: " + e.getMessage()
            ));
        }
    }


    @PostMapping("/private/join")
    public ResponseEntity<?> joinPrivateLobby(@RequestBody JoinPrivateLobbyRequest request,
                                             Principal principal, HttpServletRequest httpRequest) {
        // Verwende Session-ID als eindeutige Spieler-ID falls kein Principal vorhanden
        String playerId = principal != null ? principal.getName() :
                         httpRequest.getSession().getId();

        String playerName = userService.getUsernameById(playerId);

        System.out.println("DEBUG: Join lobby request from player: " + playerId);
        System.out.println("DEBUG: Trying to join lobby: " + request.getLobbyCode());

        // Prüfe ob Spieler bereits in einer Lobby ist
        String existingLobby = lobbyService.getPlayerLobby(playerId);
        if (existingLobby != null) {
            System.out.println("DEBUG: Player already in lobby: " + existingLobby + ", cleaning up");
            lobbyService.forceLeaveAllLobbies(playerId);
        }

        boolean success = lobbyService.joinPrivateLobby(playerId, request.getLobbyCode());

        if (success) {
            // WebSocket-Broadcast nach erfolgreichem Join
            lobbyService.broadcastLobbyJoined(request.getLobbyCode(), playerName);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "lobbyCode", request.getLobbyCode(),
                "message", "Erfolgreich der Lobby beigetreten",
                "playerId", playerName  // Für Debug
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Konnte Lobby nicht beitreten"
            ));
        }
    }


    @PostMapping("/leave")
    public ResponseEntity<?> leaveLobby(@RequestBody LeaveLobbyRequest request, Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";

        // WebSocket-Broadcast vor dem Verlassen
        lobbyService.broadcastPlayerLeft(request.getLobbyId(), playerId);
        
        lobbyService.leaveLobby(playerId, request.getLobbyId());

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Lobby verlassen"
        ));
    }


    @GetMapping("/{lobbyId}")
    public ResponseEntity<?> getLobbyInfo(@PathVariable String lobbyId) {
        var lobby = lobbyService.getLobby(lobbyId);

        if (lobby == null) {
            return ResponseEntity.notFound().build();
        }

        List<String> playerNames = new ArrayList<String>();
        for(int i = 0; i<lobby.getPlayers().size(); i++){
            playerNames.add(userService.getUsernameById(lobby.getPlayers().get(i)));
        }

        return ResponseEntity.ok(Map.of(
            "lobbyId", lobby.getLobbyId(),
            "lobbyType", lobby.getLobbyType().toString(),
            "players", playerNames,
            "status", lobby.getStatus().toString(),
            "gameTime", lobby.getGameTime() != null ? lobby.getGameTime().toString() : "null",
            "isGameStarted", lobby.isGameStarted(),
            "creator", userService.getUsernameById(lobby.getCreator())
        ));
    }



    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanupPlayer(Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";

        lobbyService.forceLeaveAllLobbies(playerId);
        lobbyService.cleanupStaleLobbies();

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Spieler aus allen Lobbys entfernt"
        ));
    }

    // Request DTOs
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickJoinRequest {
        private GameTime gameTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinPrivateLobbyRequest {
        private String lobbyCode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaveLobbyRequest {
        private String lobbyId;
    }
}
