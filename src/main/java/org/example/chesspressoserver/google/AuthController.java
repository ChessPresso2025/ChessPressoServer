package org.example.chesspressoserver.google;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;


import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleAuthService authService;

    public AuthController(GoogleAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, String>> authenticate(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");

        return authService.verifyToken(idToken)
                .map(payload ->{
                    String email = (String) payload.get("email");
                    String name = (String) payload.get("name");
                    String googleId = (String) payload.get("sub");

                    return ResponseEntity.ok(Map.of(
                            "email", email,
                            "name", name,
                            "googleId", googleId
                    ));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid ID token")));

    }
}
