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
    GoogleAuthService(String clientId, GoogleIdTokenVerifier testVerifier) {
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
        // Prüfe zuerst, ob es ein alternatives Token-Format ist
        if (idTokenString.startsWith("google_account_")) {
            return parseAlternativeToken(idTokenString);
        }

        // Normale Google JWT Token-Verifikation
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

    /**
     * Parst alternative Token im Format: "google_account_[ACCOUNT_ID]_[EMAIL]"
     * und erstellt ein Mock-Payload-Objekt mit den extrahierten Daten
     */
    private Optional<GoogleIdToken.Payload> parseAlternativeToken(String token) {
        try {
            // Format: "google_account_104988664958231654178_michael.krametter@gmail.com"
            if (!token.startsWith("google_account_")) {
                return Optional.empty();
            }

            // Entferne das Präfix
            String withoutPrefix = token.substring("google_account_".length());

            // Finde den letzten Unterstrich vor der Email
            int lastUnderscoreIndex = withoutPrefix.lastIndexOf('_');
            if (lastUnderscoreIndex == -1) {
                logger.log(Level.WARNING, "Invalid alternative token format: missing email separator");
                return Optional.empty();
            }

            String accountId = withoutPrefix.substring(0, lastUnderscoreIndex);
            String email = withoutPrefix.substring(lastUnderscoreIndex + 1);

            // Validiere die extrahierten Daten
            if (accountId.isEmpty() || email.isEmpty() || !email.contains("@")) {
                logger.log(Level.WARNING, "Invalid alternative token format: invalid account ID or email");
                return Optional.empty();
            }

            // Extrahiere den Namen aus der Email (Teil vor @)
            String name = email.substring(0, email.indexOf("@"));

            // Erstelle ein Mock-Payload-Objekt
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.set("sub", accountId);          // Google Account ID
            payload.set("email", email);            // Email-Adresse
            payload.set("name", name);              // Name (aus Email extrahiert)
            payload.set("email_verified", true);    // Als verifiziert markieren

            logger.info("Successfully parsed alternative token for account: " + accountId + ", email: " + email);
            return Optional.of(payload);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to parse alternative token: " + token, e);
            return Optional.empty();
        }
    }
}