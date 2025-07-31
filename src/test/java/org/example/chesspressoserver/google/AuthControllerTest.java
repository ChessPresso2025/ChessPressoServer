package org.example.chesspressoserver.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleAuthService googleAuthService;

    @Test
    void returnsUserDataOnValidToken() throws Exception {
        GoogleIdToken.Payload payload = Mockito.mock(GoogleIdToken.Payload.class);
        when(payload.get("email")).thenReturn("test@example.com");
        when(payload.get("name")).thenReturn("Max Testmann");
        when(payload.get("sub")).thenReturn("google-id-123");

        doReturn(Optional.of(payload)).when(googleAuthService).verifyToken("valid-token");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"valid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Max Testmann"))
                .andExpect(jsonPath("$.googleId").value("google-id-123"));
    }

    @Test
    void returnsUnauthorizedOnInvalidToken() throws Exception {
        when(googleAuthService.verifyToken("invalid-token")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"invalid-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid ID token"));
    }
}