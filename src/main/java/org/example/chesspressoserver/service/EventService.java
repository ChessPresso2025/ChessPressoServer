package org.example.chesspressoserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chesspressoserver.dto.EventRequest;
import org.example.chesspressoserver.models.GameEvent;
import org.example.chesspressoserver.repository.GameEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final GameEventRepository gameEventRepository;

    @Transactional
    public GameEvent saveEvent(UUID userId, EventRequest request) {
        GameEvent event = GameEvent.builder()
                .userId(userId)
                .type(request.getType())
                .payload(request.getPayload())
                .build();

        GameEvent savedEvent = gameEventRepository.save(event);
        log.debug("Saved event {} for user {}", savedEvent.getId(), userId);

        return savedEvent;
    }

    public Page<GameEvent> getUserEvents(UUID userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return gameEventRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
