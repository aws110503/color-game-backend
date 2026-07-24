package com.colorgame.backend.game;

import com.colorgame.backend.dto.NextRoundParamsDTO;
import com.colorgame.backend.dto.RoundResultDTO;
import com.colorgame.backend.dto.RoundStartRequest;
import com.colorgame.backend.dto.RoundSubmitRequest;
import com.colorgame.backend.model.GameRound;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startRound(@RequestBody RoundStartRequest request, Authentication authentication) {
        try {
            GameRound round = gameService.startRound(authentication.getName(), request);
            return ResponseEntity.ok(Map.of(
                    "roundId", round.getId(),
                    "gridSize", round.getGridSize(),
                    "exposureTime", round.getExposureTime(),
                    "targetGrid", round.getTargetGrid()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitRound(@RequestBody RoundSubmitRequest request, Authentication authentication) {
        try {
            RoundResultDTO result = gameService.submitRound(authentication.getName(), request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<GameRound>> getHistory(Authentication authentication) {
        return ResponseEntity.ok(gameService.getHistory(authentication.getName()));
    }

    @GetMapping("/next-params")
    public ResponseEntity<NextRoundParamsDTO> getNextParams(Authentication authentication) {
        return ResponseEntity.ok(gameService.getNextParams(authentication.getName()));
    }
}