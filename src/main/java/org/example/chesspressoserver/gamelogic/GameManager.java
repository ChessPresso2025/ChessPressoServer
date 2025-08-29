package org.example.chesspressoserver.gamelogic;

import org.example.chesspressoserver.controller.GameMessageController;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameManager {
    private final Map<String, GameMessageController> games = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GameManager(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = null;
    }

    public void startGame(String lobbyId) {
        games.put(lobbyId, new GameMessageController(
            new GameController(),
            messagingTemplate
        ));
    }

    public GameMessageController getGameByLobby(String lobbyId) {
        return games.get(lobbyId);
    }

    public boolean resignGame(String lobbyId) {
        if (games.containsKey(lobbyId)) {
            games.remove(lobbyId);
            return true;
        }
        return false;
    }

    public boolean rematch(String lobbyId) {
        if (games.containsKey(lobbyId)) {
            games.put(lobbyId, new GameMessageController(
                new GameController(),
                messagingTemplate
            ));
            return true;
        }
        return false;
    }
}
