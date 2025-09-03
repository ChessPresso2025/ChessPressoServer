package org.example.chesspressoserver.models;

public enum GameTime {
    SHORT(300),    // 5 Minuten
    MIDDLE(900),   // 15 Minuten
    LONG(1800),    // 30 Minuten
    UNLIMITED(-1); // Unbegrenzt

    private final int seconds;

    GameTime(int seconds) {
        this.seconds = seconds;
    }

}
