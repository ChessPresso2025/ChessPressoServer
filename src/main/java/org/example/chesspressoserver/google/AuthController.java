package org.example.chesspressoserver.google;

import org.example.chesspressoserver.models.*;
import org.example.chesspressoserver.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final PlayerService playerService;

    public AuthController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        logger.info("Login-Anfrage empfangen - Benutzername: {}", request.getUsername());


        try {
            if (request.getUsername() == null || request.getPassword() == null) {
                logger.warn("Login fehlgeschlagen: Benutzername oder Passwort ist null");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Player player = playerService.authenticatePlayer(request.getUsername(), request.getPassword());

            if (player == null) {
                logger.warn("Login fehlgeschlagen: Ungültige Anmeldedaten für Benutzer: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            logger.info("Login erfolgreich für Benutzer: {}", request.getUsername());
            AuthResponse response = new AuthResponse(
                    player.getPlayerId(),
                    player.getName(),
                    player.getPlayedGames(),
                    player.getWin(),
                    player.getDraw(),
                    player.getLose(),
                    player.getEmail()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Fehler beim Login für Benutzer: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        logger.info("Registrierungs-Anfrage empfangen - Benutzername: {}, Email: {}",
                   request.getUsername(), request.getEmail());

        try {
            if (request.getUsername() == null || request.getPassword() == null || request.getEmail() == null) {
                logger.warn("Registrierung fehlgeschlagen: Pflichtfelder sind null");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            Player player = playerService.registerPlayer(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail()
            );

            logger.info("Registrierung erfolgreich für Benutzer: {}", request.getUsername());
            AuthResponse response = new AuthResponse(
                    player.getPlayerId(),
                    player.getName(),
                    player.getPlayedGames(),
                    player.getWin(),
                    player.getDraw(),
                    player.getLose(),
                    player.getEmail()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Registrierung fehlgeschlagen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // Username already exists
        } catch (Exception e) {
            logger.error("Fehler bei der Registrierung für Benutzer: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
