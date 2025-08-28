package org.example.chesspressoserver.service;

import org.example.chesspressoserver.models.User;
import org.example.chesspressoserver.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Ruft den Benutzernamen für eine gegebene Benutzer-ID ab
     */
    public String getUsernameById(String userId) {
        if (userId == null) {
            return null;
        }

        try {
            UUID userUuid = UUID.fromString(userId);
            Optional<User> user = userRepository.findById(userUuid);
            return user.map(User::getUsername).orElse(userId); // Fallback auf ID
        } catch (IllegalArgumentException e) {
            // Falls userId keine gültige UUID ist, gib sie als Fallback zurück
            return userId;
        }
    }


    public Optional<User> getUserById(String userId) {
        try {
            UUID userUuid = UUID.fromString(userId);
            return userRepository.findById(userUuid);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }


    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }


    /**
     * Überprüft, ob ein Benutzer mit der gegebenen ID existiert
     */
    public boolean userExists(String userId) {
        if (userId == null) {
            return false;
        }

        try {
            UUID userUuid = UUID.fromString(userId);
            return userRepository.existsById(userUuid);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
