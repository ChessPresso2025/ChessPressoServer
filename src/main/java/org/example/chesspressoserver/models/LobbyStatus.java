package org.example.chesspressoserver.models;

public enum LobbyStatus {
    WAITING,     // Wartet auf Spieler
    FULL,        // Lobby ist voll, wartet auf Spielstart
    IN_GAME,     // Spiel läuft
    CLOSED       // Lobby geschlossen
}
