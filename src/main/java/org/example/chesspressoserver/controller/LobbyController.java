package org.example.chesspressoserver.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chesspressoserver.models.GameTime;
import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.service.UserService;
import org.example.chesspressoserver.service.GameStartHandler;
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

    private static final String SUCCESS_KEY = "success";
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String LOBBY_ID_KEY = "lobbyId";

    public LobbyController(LobbyService lobbyService, UserService userService, GameStartHandler gameStartHandler) {
        this.lobbyService = lobbyService;
        this.userService = userService;
        // Setze GameStartHandler im LobbyService für automatischen Spielstart
        this.lobbyService.setGameStartHandler(gameStartHandler);
    }


    @PostMapping("/quick-join")
    public ResponseEntity<Map<String, Object>> quickJoin(@RequestBody QuickJoinRequest request, Principal principal, HttpServletRequest httpRequest) {
        // Verwende Session-ID als eindeutige Spieler-ID falls keine vorhanden
        String playerId = principal != null ? principal.getName() :
                         httpRequest.getSession().getId();


        // Prüfe ob Spieler bereits in einer Lobby ist
        String existingLobby = lobbyService.getPlayerLobby(playerId);
        if (existingLobby != null) {
            // Prüfe ob die existierende Lobby noch aktiv und nicht im Spiel ist
            var lobby = lobbyService.getLobby(existingLobby);
            if (lobby != null && !lobby.isGameStarted()) {
                // Spieler ist in einer wartenden Lobby - Frontend-konforme Error Response
                return ResponseEntity.badRequest().body(Map.of(
                    SUCCESS_KEY, false,
                    ERROR_KEY, "Du bist bereits in einer Lobby",
                    MESSAGE_KEY, "Verlasse zuerst deine aktuelle Lobby",
                    LOBBY_ID_KEY, existingLobby
                ));
            } else {
                // Lobby existiert nicht mehr oder Spiel ist beendet - räume auf
                lobbyService.forceLeaveAllLobbies(playerId);
            }
        }

        try {
            String lobbyId = lobbyService.joinQuickMatch(playerId, request.getGameTime());
            return ResponseEntity.ok(Map.of(
                SUCCESS_KEY, true,
                LOBBY_ID_KEY, lobbyId,
                MESSAGE_KEY, "Quick Match gestartet",
                "gameTime", request.getGameTime().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                SUCCESS_KEY, false,
                ERROR_KEY, "Fehler beim Quick Match: " + e.getMessage(),
                MESSAGE_KEY, "Bitte versuche es erneut"
            ));
        }
    }


    /**
     * Private Lobby erstellen
     */
    @PostMapping("/private/create")
    public ResponseEntity<Map<String, Object>> createPrivateLobby(Principal principal, HttpServletRequest request) {
        // Verwende Session-ID als eindeutige Spieler-ID falls kein Principal vorhanden
        String playerId = principal != null ? principal.getName() :
                         request.getSession().getId();

        String playerName = userService.getUsernameById(playerId);

        // Prüfe ob Spieler bereits in einer Lobby ist
        String existingLobby = lobbyService.getPlayerLobby(playerId);
        if (existingLobby != null) {
            lobbyService.forceLeaveAllLobbies(playerId);
        }

        try {
            String lobbyCode = lobbyService.createPrivateLobby(playerId);
            return ResponseEntity.ok(Map.of(
                SUCCESS_KEY, true,
                "lobbyCode", lobbyCode,
                MESSAGE_KEY, "Private Lobby erstellt",
                "playerId", playerName  // Für Debug
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                ERROR_KEY, "Fehler beim Erstellen der Lobby: " + e.getMessage()
            ));
        }
    }


    @PostMapping("/private/join")
    public ResponseEntity<Map<String, Object>> joinPrivateLobby(@RequestBody JoinPrivateLobbyRequest request,
                                             Principal principal, HttpServletRequest httpRequest) {
        // Verwende Session-ID als eindeutige Spieler-ID falls kein Principal vorhanden
        String playerId = principal != null ? principal.getName() :
                         httpRequest.getSession().getId();

        String playerName = userService.getUsernameById(playerId);

        // Prüfe ob Spieler bereits in einer Lobby ist
        String existingLobby = lobbyService.getPlayerLobby(playerId);
        if (existingLobby != null) {
            lobbyService.forceLeaveAllLobbies(playerId);
        }

        boolean success = lobbyService.joinPrivateLobby(playerId, request.getLobbyCode());

        if (success) {
            // WebSocket-Broadcast nach erfolgreichem Join
            lobbyService.broadcastLobbyJoined(request.getLobbyCode(), playerName);
            
            return ResponseEntity.ok(Map.of(
                SUCCESS_KEY, true,
                "lobbyCode", request.getLobbyCode(),
                MESSAGE_KEY, "Erfolgreich der Lobby beigetreten",
                "playerId", playerName  // Für Debug
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                ERROR_KEY, "Konnte Lobby nicht beitreten"
            ));
        }
    }


    @PostMapping("/leave")
    public ResponseEntity<Map<String, Object>> leaveLobby(@RequestBody LeaveLobbyRequest request, Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";

        // WebSocket-Broadcast vor dem Verlassen
        lobbyService.broadcastPlayerLeft(request.getLobbyId(), playerId);
        
        lobbyService.leaveLobby(playerId, request.getLobbyId());

        return ResponseEntity.ok(Map.of(
            SUCCESS_KEY, true,
            MESSAGE_KEY, "Lobby verlassen"
        ));
    }


    @GetMapping("/{lobbyId}")
    public ResponseEntity<Map<String, Object>> getLobbyInfo(@PathVariable String lobbyId) {
        var lobby = lobbyService.getLobby(lobbyId);

        if (lobby == null) {
            return ResponseEntity.notFound().build();
        }

        List<String> playerNames = new ArrayList<String>();
        for(int i = 0; i<lobby.getPlayers().size(); i++){
            playerNames.add(userService.getUsernameById(lobby.getPlayers().get(i)));
        }

        return ResponseEntity.ok(Map.of(
            LOBBY_ID_KEY, lobby.getLobbyId(),
            "lobbyType", lobby.getLobbyType().toString(),
            "players", playerNames,
            "status", lobby.getStatus().toString(),
            "gameTime", lobby.getGameTime() != null ? lobby.getGameTime().toString() : "null",
            "isGameStarted", lobby.isGameStarted(),
            "creator", userService.getUsernameById(lobby.getCreator())
        ));
    }



    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupPlayer(Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";

        lobbyService.forceLeaveAllLobbies(playerId);
        lobbyService.cleanupStaleLobbies();

        return ResponseEntity.ok(Map.of(
            SUCCESS_KEY, true,
            MESSAGE_KEY, "Spieler aus allen Lobbys entfernt"
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
