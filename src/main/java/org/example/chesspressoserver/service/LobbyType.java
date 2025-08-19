package org.example.chesspressoserver.service;

public enum LobbyType {
    PRIVATE(6),
    PUBLIC(12);

    private final int codeLength;

    LobbyType(int codeLength) {
        this.codeLength = codeLength;
    }

    public int getCodeLength() {
        return codeLength;
    }
}
