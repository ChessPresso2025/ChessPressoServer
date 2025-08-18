package org.example.chesspressoserver.google;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class GoogleAuthService {

    private static final Logger logger = Logger.getLogger(GoogleAuthService.class.getName());
    private final GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleAuthService(@Value("${google.client-id}") String clientId) throws GeneralSecurityException, IOException {
        this.verifier = createVerifier(clientId);
    }

    // Zusätzlicher Konstruktor für Tests
    GoogleAuthService(GoogleIdTokenVerifier testVerifier) {
        this.verifier = testVerifier;
    }

    private static GoogleIdTokenVerifier createVerifier(String clientId) throws GeneralSecurityException, IOException {
        return new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public Optional<GoogleIdToken.Payload> verifyToken(String idTokenString) {
        // Nur echte Google JWT Token-Verifikation
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                return Optional.of(payload);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to verify Google ID token", e);
        }
        return Optional.empty();
    }
}