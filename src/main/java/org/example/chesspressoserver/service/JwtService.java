package org.example.chesspressoserver.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    private MACSigner signer;
    private MACVerifier verifier;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT_SECRET environment variable is required but not set");
        }

        try {
            byte[] secretBytes = jwtSecret.getBytes();
            this.signer = new MACSigner(secretBytes);
            this.verifier = new MACVerifier(secretBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT service", e);
        }
    }

    public String generateToken(UUID userId, String username) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plusMillis(jwtExpiration);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim("uname", username)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiration))
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claims
            );

            signedJWT.sign(signer);
            return signedJWT.serialize();

        } catch (JOSEException e) {
            System.err.println("Error generating JWT token: " + e.getMessage());
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    public UUID getUserIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Invalid JWT token signature");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Check expiration
            if (claims.getExpirationTime().before(new Date())) {
                throw new RuntimeException("JWT token has expired");
            }

            return UUID.fromString(claims.getSubject());

        } catch (ParseException | JOSEException e) {
            System.err.println("Error parsing JWT token: " + e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Invalid JWT token signature");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Check expiration
            if (claims.getExpirationTime().before(new Date())) {
                throw new RuntimeException("JWT token has expired");
            }

            return claims.getStringClaim("uname");

        } catch (ParseException | JOSEException e) {
            System.err.println("Error parsing JWT token: " + e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            getUserIdFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpirationInSeconds() {
        return jwtExpiration / 1000;
    }
}
