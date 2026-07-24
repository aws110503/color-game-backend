package com.colorgame.backend.controller;

import com.colorgame.backend.dto.HistoryDTO;
import com.colorgame.backend.dto.HistoryRequest;
import com.colorgame.backend.service.HistoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @PostMapping("/api/history")
    public ResponseEntity<?> recordChange(Authentication auth, @Valid @RequestBody HistoryRequest request) {
        String username = auth.getName();
        HistoryDTO dto = historyService.recordChange(username, request);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/api/history/me")
    public ResponseEntity<List<HistoryDTO>> getMyHistory(Authentication auth) {
        String username = auth.getName();
        return ResponseEntity.ok(historyService.getMyHistory(username));
    }

    @GetMapping("/api/admin/history/all")
    public ResponseEntity<List<HistoryDTO>> getAllHistory() {
        return ResponseEntity.ok(historyService.getAllHistory());
    }
}