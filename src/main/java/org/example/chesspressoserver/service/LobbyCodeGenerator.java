package org.example.chesspressoserver.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class LobbyCodeGenerator {
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789";
    private static final SecureRandom random = new SecureRandom();

    // Liste zum Speichern aller verwendeten Lobby-Codes
    private final List<String> usedLobbyIds = new ArrayList<>();


    public String generateLobbyCode(LobbyType lobbyType) {
        String code;

        do {
            code = generateRandomCode(lobbyType.getCodeLength());
        } while (usedLobbyIds.contains(code));

        // Code zur Liste hinzufügen
        usedLobbyIds.add(code);
        return code;
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
        return usedLobbyIds.contains(code);
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
