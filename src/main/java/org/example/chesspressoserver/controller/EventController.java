package org.example.chesspressoserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chesspressoserver.dto.EventRequest;
import org.example.chesspressoserver.models.GameEvent;
import org.example.chesspressoserver.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<GameEvent> saveEvent(@Valid @RequestBody EventRequest request,
                                              Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        GameEvent savedEvent = eventService.saveEvent(userId, request);
        return ResponseEntity.ok(savedEvent);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<GameEvent>> getMyEvents(@RequestParam(defaultValue = "50") int limit,
                                                      Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Page<GameEvent> events = eventService.getUserEvents(userId, limit);
        return ResponseEntity.ok(events);
    }
}
