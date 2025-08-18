package org.example.chesspressoserver.service;

import org.example.chesspressoserver.models.Player;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlayerService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Map<String, Player> playersByUsername = new ConcurrentHashMap<>();

    /**
     * Registriert einen neuen Player
     * @param username Der Benutzername
     * @param password Das Passwort (in einer echten Anwendung sollte dies gehasht werden)
     * @param email Die E-Mail-Adresse
     * @return Der neue Player
     * @throws IllegalArgumentException wenn der Benutzername bereits existiert
     */
    public Player registerPlayer(String username, String password, String email) {
        logger.info("Versuche Player zu registrieren: username={}, email={}", username, email);

        if (playersByUsername.containsKey(username)) {
            logger.warn("Registrierung fehlgeschlagen: Benutzername '{}' bereits vergeben", username);
            throw new IllegalArgumentException("Benutzername bereits vergeben");
        }

        String playerId = UUID.randomUUID().toString();
        Player player = new Player(playerId, username, username, password, email);

        players.put(playerId, player);
        playersByUsername.put(username, player);

        logger.info("Player erfolgreich registriert: username={}, playerId={}", username, playerId);
        return player;
    }

    /**
     * Authentifiziert einen Player mit Benutzername und Passwort
     * @param username Der Benutzername
     * @param password Das Passwort
     * @return Der Player falls Authentifizierung erfolgreich, null sonst
     */
    public Player authenticatePlayer(String username, String password) {
        logger.info("Versuche Player zu authentifizieren: username={}", username);
        logger.debug("Aktuell registrierte Benutzer: {}", playersByUsername.keySet());

        Player player = playersByUsername.get(username);
        if (player == null) {
            logger.warn("Authentifizierung fehlgeschlagen: Benutzer '{}' nicht gefunden", username);
            return null;
        }

        if (player.getPassword().equals(password)) {
            logger.info("Authentifizierung erfolgreich für Benutzer: {}", username);
            return player;
        } else {
            logger.warn("Authentifizierung fehlgeschlagen: Falsches Passwort für Benutzer: {}", username);
            return null;
        }
    }

    /**
     * Findet oder erstellt einen Player basierend auf der Google ID (für Rückwärtskompatibilität)
     * @param googleId Die Google ID des Benutzers
     * @param name Der Name des Benutzers
     * @return Der interne Player
     */
    public Player findOrCreatePlayer(String googleId, String name) {
        return players.computeIfAbsent(googleId, id -> new Player(id, name));
    }
    
    /**
     * Sucht einen Player anhand der Player ID
     * @param playerId Die Player ID
     * @return Der Player oder null falls nicht gefunden
     */
    public Player findPlayerById(String playerId) {
        return players.get(playerId);
    }

    /**
     * Sucht einen Player anhand des Benutzernamens
     * @param username Der Benutzername
     * @return Der Player oder null falls nicht gefunden
     */
    public Player findPlayerByUsername(String username) {
        return playersByUsername.get(username);
    }
    
    /**
     * Aktualisiert den Namen eines Players
     * @param playerId Die Player ID
     * @param name Der neue Name
     */
    public void updatePlayerName(String playerId, String name) {
        Player player = players.get(playerId);
        if (player != null) {
            player.setName(name);
        }
    }
}