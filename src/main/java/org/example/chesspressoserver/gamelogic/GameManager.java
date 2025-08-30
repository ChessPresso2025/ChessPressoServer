package org.example.chesspressoserver.gamelogic;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameManager {
    private final Map<String, GameController> games = new ConcurrentHashMap<>();

    public void startGame(String lobbyId) {
        games.put(lobbyId, new GameController());
    }

    public GameController getGameByLobby(String lobbyId) {
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
            games.put(lobbyId, new GameController());
            return true;
        }
        return false;
    }
}
