package org.example.chesspressoserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chesspressoserver.dto.StatsReportRequest;
import org.example.chesspressoserver.dto.StatsResponse;
import org.example.chesspressoserver.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/report")
    public ResponseEntity<Void> reportGameResult(@Valid @RequestBody StatsReportRequest request,
                                                Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        statsService.reportGameResult(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<StatsResponse> getMyStats(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        StatsResponse stats = statsService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }
}
