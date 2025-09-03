package org.example.chesspressoserver.service;

import lombok.Setter;
import org.example.chesspressoserver.models.Lobby.LobbyType;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
public class LobbyCodeGenerator {
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789";
    private static final SecureRandom random = new SecureRandom();

    // Liste zum Speichern aller verwendeten Lobby-Codes
    private final List<String> usedLobbyIds = new ArrayList<>();

    // Callback-Interface um aktive Lobbys zu prüfen
    @Setter
    private Function<String, Boolean> lobbyExistsChecker;

    public String generateLobbyCode(LobbyType lobbyType) {
        String code;


        int codeLength = (lobbyType == LobbyType.PRIVATE) ? 6 : 12;
        
        do {
            code = generateRandomCode(codeLength);
            System.out.println("DEBUG: Generated candidate code: " + code);
        } while (isCodeInUse(code));

        // Code zur Liste hinzufügen
        usedLobbyIds.add(code);
        return code;
    }

    private boolean isCodeInUse(String code) {
        // Prüfe zuerst die lokale Liste
        if (usedLobbyIds.contains(code)) {
            return true;
        }

        // Prüfe auch die tatsächlich aktiven Lobbys falls Checker verfügbar
        if (lobbyExistsChecker != null) {
            return lobbyExistsChecker.apply(code);
        }

        return false;
    }


    public String generatePrivateLobbyCode() {
        return generateLobbyCode(LobbyType.PRIVATE);
    }


    public String generatePublicLobbyCode() {
        return generateLobbyCode(LobbyType.PUBLIC);
    }


    private String generateRandomCode(int length) {
        StringBuilder lobbyCode = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            lobbyCode.append(CHARACTERS.charAt(index));
        }
        return lobbyCode.toString();
    }


    public boolean isCodeUsed(String code) {
        // Prüfe sowohl die lokale Liste als auch die aktiven Lobbys
        return usedLobbyIds.contains(code) || (lobbyExistsChecker != null && lobbyExistsChecker.apply(code));
    }


    public void removeLobbyCode(String code) {
        usedLobbyIds.remove(code);
    }

    public List<String> getAllUsedLobbyCodes() {
        return new ArrayList<>(usedLobbyIds);
    }


    public int getActiveLobbyCount() {
        return usedLobbyIds.size();
    }
}
