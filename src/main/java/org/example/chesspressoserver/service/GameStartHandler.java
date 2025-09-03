package org.example.chesspressoserver.service;

import org.example.chesspressoserver.models.requests.StartGameRequest;

public interface GameStartHandler {
    void startGame(StartGameRequest request);
}

