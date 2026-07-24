package com.colorgame.backend.game;

import com.colorgame.backend.ai.AIService;
import com.colorgame.backend.ai.PatternResponse;
import com.colorgame.backend.ai.PromptBuilder;
import com.colorgame.backend.ai.ScoringResponse;
import com.colorgame.backend.dto.NextRoundParamsDTO;
import com.colorgame.backend.dto.RoundResultDTO;
import com.colorgame.backend.dto.RoundStartRequest;
import com.colorgame.backend.dto.RoundSubmitRequest;
import com.colorgame.backend.model.CellAttempt;
import com.colorgame.backend.model.GameRound;
import com.colorgame.backend.model.User;
import com.colorgame.backend.profile.ProfileService;
import com.colorgame.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// @Service tells the app: "This file is a Worker Bee that handles the core game logic."
@Service
public class GameService {

    // These are placeholders for the tools this service needs to do its job
    private final GameRoundRepository gameRoundRepository; // To save/load game rounds
    private final CellAttemptRepository cellAttemptRepository; // To save individual square clicks
    private final UserRepository userRepository; // To find player accounts
    private final AIService aiService; // To talk to the AI
    private final ProfileService profileService; // PILIER 3 — to update the player's persistent profile after each round
    private final ObjectMapper objectMapper = new ObjectMapper(); // A tool to convert data grids into plain text strings

    // PILIER 2 — Seuils utilisés pour décider du prochain niveau de difficulté
    private static final int MIN_ROUNDS_FOR_ADAPTATION = 3; // Pas assez de données avant ce nombre de rounds -> reste en BEGINNER
    private static final double ADVANCED_THRESHOLD = 80.0;      // Score moyen >= 80 -> ADVANCED
    private static final double INTERMEDIATE_THRESHOLD = 50.0;  // Score moyen >= 50 -> INTERMEDIATE

    // This is the "Constructor" — it hooks up all the tools mentioned above so they are ready to use
    public GameService(GameRoundRepository gameRoundRepository,
                        CellAttemptRepository cellAttemptRepository,
                        UserRepository userRepository,
                        AIService aiService,
                        ProfileService profileService) {
        this.gameRoundRepository = gameRoundRepository;
        this.cellAttemptRepository = cellAttemptRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.profileService = profileService;
    }

    // ACTION 1: Start a new round of the game
    public GameRound startRound(String username, RoundStartRequest request) {
        // Look up the player by their username. If they don't exist, stop and scream "User not found!"
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        // PILIER 2 : la difficulté s'adapte maintenant à l'historique récent du joueur
        // (au lieu d'être toujours fixée à "BEGINNER" comme au Pilier 1)
        String difficulty = determineDifficulty(username);

        // Ask the AI tool to generate a secret color pattern based on the grid size and difficulty
        PatternResponse pattern = aiService.generatePattern(request.getGridSize(), difficulty);

        // Figure out the width and height numbers from the grid size text (e.g., "4x4" becomes 4 and 4)
        int[] dims = PromptBuilder.parseGridSize(request.getGridSize());
        
        // Calculate how many seconds the player gets to look at the grid based on its size
        int exposureTime = calculateExposureTime(dims[0], dims[1]);

        // Create a blank "Game Round" record sheet to fill out
        GameRound round = new GameRound();
        round.setUser(user); // Link this round to the player
        round.setGridSize(request.getGridSize()); // Save the grid size
        round.setExposureTime(exposureTime); // Save the timer duration
        round.setPlayerScore(0); // Set initial score to 0 (will change when they submit)

        try {
            // Take the AI's secret grid pattern and convert it to a text format to save in the database
            round.setTargetGrid(objectMapper.writeValueAsString(pattern.getGrid()));
        } catch (Exception e) {
            // If something breaks while saving the text, crash safely with an error message
            throw new RuntimeException("Erreur lors de la sauvegarde du pattern généré.", e);
        }

        // Put the finished round record into the database filing cabinet and return it to the player's screen
        return gameRoundRepository.save(round);
    }

    // ACTION 2: Grade the player's attempt when they hit "Submit"
    public RoundResultDTO submitRound(String username, RoundSubmitRequest request) {
        // Make sure the player still exists in our database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        // Find the specific game round they are trying to submit. If it doesn't match the user, error out!
        GameRound round = gameRoundRepository.findByIdAndUser(request.getRoundId(), user)
                .orElseThrow(() -> new RuntimeException("Round introuvable pour cet utilisateur."));

        // Prepare a blank grid to read the original "answer key" pattern into
        List<List<String>> targetGrid;
        try {
            // Convert the saved text format back into a readable grid of colors
            targetGrid = objectMapper.readValue(round.getTargetGrid(), List.class);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture du pattern original.", e);
        }

        String targetGridJson;
        String playerGridJson;
        try {
            // Convert both the Answer Key and the Player's Guess into text format so we can send them to the AI
            targetGridJson = objectMapper.writeValueAsString(targetGrid);
            playerGridJson = objectMapper.writeValueAsString(request.getPlayerGrid());
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la préparation des grilles pour l'évaluation.", e);
        }

        // Send both grids to the AI and ask it to grade the player's work
        ScoringResponse scoring = aiService.scoreAttempt(targetGridJson, playerGridJson);

        // Update our game record with what the AI said: the score, the biggest mistake, and its overall verdict text
        round.setPlayerScore(scoring.getScore());
        round.setDominantMistake(scoring.getDominantMistake());
        round.setAiVerdict(scoring.getVerdict());
        gameRoundRepository.save(round); // Save those updates to the database

        // Create two blank lists to hold visual "heatmaps" (True/False grids showing what they got right/wrong)
        List<List<Boolean>> exactHeatmap = new ArrayList<>(); // For perfect color matches
        List<List<Boolean>> familyHeatmap = new ArrayList<>(); // For matching the right color group (e.g. light blue vs dark blue)

        // Grab the grid that the player submitted
        List<List<String>> playerGrid = request.getPlayerGrid();

        // Loop through the grid row by row (r = row index)
        for (int r = 0; r < scoring.getCellResults().size(); r++) {
            List<Boolean> exactRow = new ArrayList<>(); // A row of True/Falses for exact colors
            List<Boolean> familyRow = new ArrayList<>(); // A row of True/Falses for color families

            // Loop through every single column/square inside that row (c = column index)
            for (int c = 0; c < scoring.getCellResults().get(r).size(); c++) {
                // Get the AI's grading results for this specific single square
                ScoringResponse.CellResult result = scoring.getCellResults().get(r).get(c);

                // Add the true/false results to our temporary row lists
                exactRow.add(result.getExactMatch());
                familyRow.add(result.getFamilyMatch());

                // Create a record for this specific square attempt to save individually in the database
                CellAttempt attempt = new CellAttempt();
                attempt.setRound(round); // Link it to this game round
                attempt.setCellRow(r); // Save the row number
                attempt.setCellCol(c); // Save the column number
                attempt.setTargetColor(targetGrid.get(r).get(c)); // Save what color it was supposed to be
                attempt.setPlayerColor(playerGrid.get(r).get(c)); // Save what color the player guessed
                attempt.setIsExactMatch(result.getExactMatch()); // Save if it was a perfect match
                attempt.setIsFamilyMatch(result.getFamilyMatch()); // Save if it was a family match
                cellAttemptRepository.save(attempt); // Save this square's data to the database
            }
            // Add the completed row of True/Falses to the main heatmaps
            exactHeatmap.add(exactRow);
            familyHeatmap.add(familyRow);
        }

        // PILIER 3 : met à jour le profil persistant du joueur (stats + faiblesses détectées)
        // This recalculates total games, average score, and known weaknesses using the round we just saved.
        profileService.updateAfterRound(username);

        // Package up the final report card and send it back to the game screen
        return new RoundResultDTO(
                round.getId(),
                scoring.getScore(),
                scoring.getVerdict(),
                scoring.getDominantMistake(),
                exactHeatmap,
                familyHeatmap
        );
    }

    // ACTION 3: Look up a player's match history
    public List<GameRound> getHistory(String username) {
        // Find the user. If they don't exist, throw an error.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));
        
        // Go to the database and pull all rounds played by this user, ordered from newest to oldest
        return gameRoundRepository.findByUserOrderByPlayedAtDesc(user);
    }

    // ACTION 4 (PILIER 2): Calculate the recommended difficulty for this player's next round,
    // based on their average score over their last 5 rounds. No AI call needed — pure logic.
    public NextRoundParamsDTO getNextParams(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        // Grab only the 5 most recent rounds (Spring Data JPA builds this query automatically from the method name)
        List<GameRound> recentRounds = gameRoundRepository.findTop5ByUserOrderByPlayedAtDesc(user);

        if (recentRounds.size() < MIN_ROUNDS_FOR_ADAPTATION) {
            // Pas assez de données pour adapter intelligemment : on reste prudent avec le niveau débutant
            return new NextRoundParamsDTO("BEGINNER", "3x3", null, recentRounds.size());
        }

        // Average the scores of those recent rounds (functional-style stream instead of a manual loop)
        double averageScore = recentRounds.stream()
                .mapToInt(GameRound::getPlayerScore)
                .average()
                .orElse(0.0);

        String difficulty;
        String recommendedGridSize;

        if (averageScore >= ADVANCED_THRESHOLD) {
            difficulty = "ADVANCED";
            recommendedGridSize = "5x5";
        } else if (averageScore >= INTERMEDIATE_THRESHOLD) {
            difficulty = "INTERMEDIATE";
            recommendedGridSize = "4x4";
        } else {
            difficulty = "BEGINNER";
            recommendedGridSize = "3x3";
        }

        return new NextRoundParamsDTO(difficulty, recommendedGridSize, averageScore, recentRounds.size());
    }

    // Small private helper so startRound() can reuse the exact same logic that /next-params exposes publicly.
    // This is what makes the difficulty genuinely "load-bearing": one single source of truth for both.
    private String determineDifficulty(String username) {
        return getNextParams(username).getDifficulty();
    }

    // HELPER RULE: Calculate how many seconds a player gets to view the grid
    private int calculateExposureTime(int rows, int cols) {
        int cellCount = rows * cols; // Total number of squares in the grid
        if (cellCount <= 9) return 5; // 9 squares or less? You get 5 seconds.
        if (cellCount <= 16) return 7; // 10 to 16 squares? You get 7 seconds.
        return 10; // More than 16 squares? You get 10 seconds.
    }
}