package org.example.chesspressoserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LobbyCodeGeneratorTest {

    private LobbyCodeGenerator lobbyCodeGenerator;

    @BeforeEach
    public void setUp() {
        lobbyCodeGenerator = new LobbyCodeGenerator();
    }

    @Test
    public void testGeneratePrivateLobbyCode() {
        String code = lobbyCodeGenerator.generatePrivateLobbyCode();

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(lobbyCodeGenerator.isCodeUsed(code));
    }

    @Test
    public void testGeneratePublicLobbyCode() {
        String code = lobbyCodeGenerator.generatePublicLobbyCode();

        assertNotNull(code);
        assertEquals(12, code.length());
        assertTrue(lobbyCodeGenerator.isCodeUsed(code));
    }

    @Test
    public void testUniqueness() {
        String code1 = lobbyCodeGenerator.generatePrivateLobbyCode();
        String code2 = lobbyCodeGenerator.generatePrivateLobbyCode();

        assertNotEquals(code1, code2);
        assertTrue(lobbyCodeGenerator.isCodeUsed(code1));
        assertTrue(lobbyCodeGenerator.isCodeUsed(code2));
    }

    @Test
    public void testCodeListManagement() {
        String code = lobbyCodeGenerator.generatePrivateLobbyCode();

        assertTrue(lobbyCodeGenerator.isCodeUsed(code));
        assertEquals(1, lobbyCodeGenerator.getActiveLobbyCount());

        lobbyCodeGenerator.removeLobbyCode(code);
        assertFalse(lobbyCodeGenerator.isCodeUsed(code));
        assertEquals(0, lobbyCodeGenerator.getActiveLobbyCount());
    }

    @Test
    public void testMultipleLobbyCodes() {
        String code1 = lobbyCodeGenerator.generatePrivateLobbyCode();
        String code2 = lobbyCodeGenerator.generatePublicLobbyCode();
        String code3 = lobbyCodeGenerator.generatePrivateLobbyCode();

        assertEquals(3, lobbyCodeGenerator.getActiveLobbyCount());
        assertTrue(lobbyCodeGenerator.getAllUsedLobbyCodes().contains(code1));
        assertTrue(lobbyCodeGenerator.getAllUsedLobbyCodes().contains(code2));
        assertTrue(lobbyCodeGenerator.getAllUsedLobbyCodes().contains(code3));
    }
}
