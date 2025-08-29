package org.example.chesspressoserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chesspressoserver.dto.ChangeUsernameRequest;
import org.example.chesspressoserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {
    private MockMvc mockMvc;
    private UserService userService;
    private ObjectMapper objectMapper;
    private UserController userController;

    @BeforeEach
    void setup() {
        userService = Mockito.mock(UserService.class);
        objectMapper = new ObjectMapper();
        userController = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .build();
    }

    @Test
    void changeUsername_success() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "123e4567-e89b-12d3-a456-426614174000", null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        ChangeUsernameRequest request = new ChangeUsernameRequest();
        request.setNewUsername("newUser");
        Mockito.doNothing().when(userService).changeUsername(any(UUID.class), eq("newUser"));

        mockMvc.perform(patch("/user/username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Mockito.verify(userService).changeUsername(any(UUID.class), eq("newUser"));
        SecurityContextHolder.clearContext();
    }

    @Test
    void changeUsername_usernameAlreadyExists() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "123e4567-e89b-12d3-a456-426614174000", null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        ChangeUsernameRequest request = new ChangeUsernameRequest();
        request.setNewUsername("existingUser");
        Mockito.doThrow(new IllegalArgumentException("Benutzername bereits vergeben"))
                .when(userService).changeUsername(any(UUID.class), eq("existingUser"));

        mockMvc.perform(patch("/user/username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Benutzername bereits vergeben"));

        Mockito.verify(userService).changeUsername(any(UUID.class), eq("existingUser"));
        SecurityContextHolder.clearContext();
    }

    @Test
    void changeUsername_unauthenticated() throws Exception {
        ChangeUsernameRequest request = new ChangeUsernameRequest();
        request.setNewUsername("newUser");

        mockMvc.perform(patch("/user/username")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
