package org.example.chesspressoserver.google;

import org.example.chesspressoserver.models.Player;
import org.example.chesspressoserver.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleAuthService authService;
    private final PlayerService playerService;

    public AuthController(GoogleAuthService authService, PlayerService playerService) {
        this.authService = authService;
        this.playerService = playerService;
    }

    @PostMapping("/google")
    public ResponseEntity<?> authenticate(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");

        return authService.verifyToken(idToken)
                .map(payload -> {
                    String email = (String) payload.get("email");
                    String name = (String) payload.get("name");
                    String googleId = (String) payload.get("sub");

                    // Finde oder erstelle internen Player
                    Player player = playerService.findOrCreatePlayer(googleId, name);

                    // Gebe Player-Daten zurück
                    return ResponseEntity.ok(Map.of(
                            "playerId", player.getPlayerId(),
                            "name", player.getName(),
                            "playedGames", player.getPlayedGames(),
                            "win", player.getWin(),
                            "draw", player.getDraw(),
                            "lose", player.getLose(),
                            "email", email
                    ));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid ID token")));
    }
}
