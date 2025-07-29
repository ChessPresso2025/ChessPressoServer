package org.example.chesspressoserver.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GoogleAuthServiceTest {

    private GoogleIdTokenVerifier verifier;
    private GoogleAuthService service;

    @BeforeEach
    void setup() {
        verifier = mock(GoogleIdTokenVerifier.class);
        service = new GoogleAuthService("dummy-client-id", verifier);
    }

    @Test
    void returnsPayloadForValidToken() throws Exception {

        GoogleIdToken idToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);


        when(idToken.getPayload()).thenReturn(payload);
        when(verifier.verify("valid-token")).thenReturn(idToken);

        Optional<GoogleIdToken.Payload> result = service.verifyToken("valid-token");


        assertTrue(result.isPresent());
        assertEquals(payload, result.get());
        verify(verifier).verify("valid-token");
    }

    @Test
    void returnsEmptyForInvalidToken() throws Exception {
        when(verifier.verify("invalid-token")).thenReturn(null);

        Optional<GoogleIdToken.Payload> result = service.verifyToken("invalid-token");

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyOnException() throws Exception {
        when(verifier.verify("error-token")).thenThrow(new RuntimeException("Verification failed"));

        Optional<GoogleIdToken.Payload> result = service.verifyToken("error-token");

        assertTrue(result.isEmpty());
    }
}