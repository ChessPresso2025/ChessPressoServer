package org.example.chesspressoserver.service;

import org.example.chesspressoserver.models.User;
import org.example.chesspressoserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UUID testUserId;
    private String testUserIdString;
    private String testUsername;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUserIdString = testUserId.toString();
        testUsername = "testUser";

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setUsername(testUsername);
        testUser.setEmail("test@example.com");
    }

    @Test
    void getUsernameById_WithValidUUID_ShouldReturnUsername() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        String result = userService.getUsernameById(testUserIdString);

        // Then
        assertThat(result).isEqualTo(testUsername);
        verify(userRepository).findById(testUserId);
    }

    @Test
    void getUsernameById_WithNonExistentUser_ShouldReturnUserId() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When
        String result = userService.getUsernameById(testUserIdString);

        // Then
        assertThat(result).isEqualTo(testUserIdString);
        verify(userRepository).findById(testUserId);
    }

    @Test
    void getUsernameById_WithInvalidUUID_ShouldReturnOriginalString() {
        // Given
        String invalidUUID = "not-a-valid-uuid";

        // When
        String result = userService.getUsernameById(invalidUUID);

        // Then
        assertThat(result).isEqualTo(invalidUUID);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getUsernameById_WithNullUserId_ShouldReturnNull() {
        // When
        String result = userService.getUsernameById(null);

        // Then
        assertThat(result).isNull();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getUsernameById_WithEmptyString_ShouldReturnEmptyString() {
        // When
        String result = userService.getUsernameById("");

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getUserById_WithValidUUID_ShouldReturnUser() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserById(testUserIdString);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        assertThat(result.get().getUsername()).isEqualTo(testUsername);
        verify(userRepository).findById(testUserId);
    }

    @Test
    void getUserById_WithNonExistentUser_ShouldReturnEmpty() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserById(testUserIdString);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findById(testUserId);
    }

    @Test
    void getUserById_WithInvalidUUID_ShouldReturnEmpty() {
        // Given
        String invalidUUID = "not-a-valid-uuid";

        // When
        Optional<User> result = userService.getUserById(invalidUUID);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getUserByUsername_WithExistingUsername_ShouldReturnUser() {
        // Given
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserByUsername(testUsername);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        verify(userRepository).findByUsername(testUsername);
    }

    @Test
    void getUserByUsername_WithNonExistentUsername_ShouldReturnEmpty() {
        // Given
        String nonExistentUsername = "nonExistentUser";
        when(userRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserByUsername(nonExistentUsername);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByUsername(nonExistentUsername);
    }

    @Test
    void getUserByUsername_WithNullUsername_ShouldCallRepository() {
        // Given
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserByUsername(null);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByUsername(null);
    }

    @Test
    void userExists_WithValidUUID_ShouldReturnTrue() {
        // Given
        when(userRepository.existsById(testUserId)).thenReturn(true);

        // When
        boolean result = userService.userExists(testUserIdString);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsById(testUserId);
    }

    @Test
    void userExists_WithNonExistentUser_ShouldReturnFalse() {
        // Given
        when(userRepository.existsById(testUserId)).thenReturn(false);

        // When
        boolean result = userService.userExists(testUserIdString);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsById(testUserId);
    }

    @Test
    void userExists_WithInvalidUUID_ShouldReturnFalse() {
        // Given
        String invalidUUID = "not-a-valid-uuid";

        // When
        boolean result = userService.userExists(invalidUUID);

        // Then
        assertThat(result).isFalse();
        verify(userRepository, never()).existsById(any());
    }

    @Test
    void userExists_WithNullUserId_ShouldReturnFalse() {
        // When
        boolean result = userService.userExists(null);

        // Then
        assertThat(result).isFalse();
        verify(userRepository, never()).existsById(any());
    }

    @Test
    void multipleOperations_ShouldWorkCorrectly() {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        String username1 = "user1";
        String username2 = "user2";

        User user1 = new User();
        user1.setId(userId1);
        user1.setUsername(username1);

        User user2 = new User();
        user2.setId(userId2);
        user2.setUsername(username2);

        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(userRepository.findByUsername(username1)).thenReturn(Optional.of(user1));
        when(userRepository.existsById(userId1)).thenReturn(true);
        when(userRepository.existsById(userId2)).thenReturn(false);

        // When & Then
        assertThat(userService.getUsernameById(userId1.toString())).isEqualTo(username1);
        assertThat(userService.getUsernameById(userId2.toString())).isEqualTo(username2);
        assertThat(userService.getUserById(userId1.toString())).isPresent();
        assertThat(userService.getUserByUsername(username1)).isPresent();
        assertThat(userService.userExists(userId1.toString())).isTrue();
        assertThat(userService.userExists(userId2.toString())).isFalse();
    }

    @Test
    void edgeCases_ShouldBeHandledGracefully() {
        // Test with various edge cases

        // Empty strings
        assertThat(userService.getUsernameById("")).isEmpty();
        assertThat(userService.getUserById("")).isEmpty();
        assertThat(userService.userExists("")).isFalse();

        // Whitespace
        assertThat(userService.getUsernameById("   ")).isEqualTo("   ");
        assertThat(userService.getUserById("   ")).isEmpty();
        assertThat(userService.userExists("   ")).isFalse();

        // Special characters
        String specialChars = "!@#$%^&*()";
        assertThat(userService.getUsernameById(specialChars)).isEqualTo(specialChars);
        assertThat(userService.getUserById(specialChars)).isEmpty();
        assertThat(userService.userExists(specialChars)).isFalse();
    }

    @Test
    void performanceTest_ShouldHandleMultipleCalls() {
        // Given
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        // When - simulate multiple rapid calls
        for (int i = 0; i < 100; i++) {
            userService.getUsernameById(testUserIdString);
        }

        // Then
        verify(userRepository, times(100)).findById(testUserId);
    }

    @Test
    void repositoryException_ShouldBePropagated() {
        // Given
        when(userRepository.findById(testUserId)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        try {
            userService.getUsernameById(testUserIdString);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database error");
        }
    }
}
