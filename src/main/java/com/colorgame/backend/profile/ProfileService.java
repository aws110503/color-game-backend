// This tells the computer where this file lives in the project's folder structure
package com.colorgame.backend.profile;

// These "import" lines bring in tools from other boxes into this file
import com.colorgame.backend.ai.AIService;
import com.colorgame.backend.dto.CoachingHistoryDTO;
import com.colorgame.backend.dto.CoachingResponse;
import com.colorgame.backend.dto.ProfileDTO;
import com.colorgame.backend.model.CoachingHistory;
import com.colorgame.backend.model.GameRound;
import com.colorgame.backend.model.PlayerProfile;
import com.colorgame.backend.model.User;
import com.colorgame.backend.repository.UserRepository;
import com.colorgame.backend.game.GameRoundRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

// @Service tells the app: "This file is a Worker Bee that keeps player profiles up to date
// and generates personalized coaching for them."
@Service
public class ProfileService {

    // These are placeholders for the tools this service needs to do its job
    private final PlayerProfileRepository playerProfileRepository;   // To save/load player profiles
    private final GameRoundRepository gameRoundRepository;           // To read all rounds a player has played
    private final UserRepository userRepository;                    // To find player accounts
    private final CoachingHistoryRepository coachingHistoryRepository; // To save each coaching message ever generated
    private final AIService aiService;                              // PILIER 3 Half B — to ask Gemini to write the coaching message
    private final ObjectMapper objectMapper = new ObjectMapper();   // A tool to convert lists/objects into plain text (JSON) for storage

    // RULE: a mistake only counts as a real "weakness" if it shows up at least this many times.
    // Un point faible n'est retenu que s'il apparaît au moins ce nombre de fois
    // parmi les rounds récents — évite de "punir" le joueur pour une seule erreur isolée.
    private static final int WEAKNESS_MIN_OCCURRENCES = 2;

    // This is the "Constructor" — it hooks up all the tools mentioned above so they are ready to use
    public ProfileService(PlayerProfileRepository playerProfileRepository,
                           GameRoundRepository gameRoundRepository,
                           UserRepository userRepository,
                           CoachingHistoryRepository coachingHistoryRepository,
                           AIService aiService) {
        this.playerProfileRepository = playerProfileRepository;
        this.gameRoundRepository = gameRoundRepository;
        this.userRepository = userRepository;
        this.coachingHistoryRepository = coachingHistoryRepository;
        this.aiService = aiService;
    }

    /**
     * Récupère le profil existant du joueur, ou en crée un nouveau (vide) s'il n'en a pas encore.
     * Pattern "get or create" — nécessaire car chaque joueur n'a qu'un seul profil (contrainte UNIQUE).
     */
    private PlayerProfile getOrCreateProfile(User user) {
        return playerProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    // No profile found for this player yet — start a fresh one
                    PlayerProfile newProfile = new PlayerProfile();
                    newProfile.setUser(user); // Link it to this player
                    return newProfile; // sera sauvegardé par l'appelant (not saved yet — caller saves it)
                });
    }

    /**
     * Appelée après chaque round soumis (voir GameService.submitRound).
     * Recalcule les statistiques globales du joueur à partir de TOUS ses rounds joués.
     */
    public void updateAfterRound(String username) {
        // Make sure the player still exists in our database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        // Pull EVERY round this player has ever played — total games and lifetime average need
        // the full history, unlike Pilier 2's difficulty logic, which only looks at the last 5 rounds.
        List<GameRound> allRounds = gameRoundRepository.findByUserOrderByPlayedAtDesc(user);

        // Grab their existing profile, or start a new blank one if this is their first round ever
        PlayerProfile profile = getOrCreateProfile(user);

        // Update the simple counter: how many rounds have they played in total?
        profile.setTotalGames(allRounds.size());

        // Calculate their average score across every round they've ever played
        double average = allRounds.stream()
                .mapToInt(GameRound::getPlayerScore) // Pull just the score number out of each round
                .average() // Average them all together
                .orElse(0.0); // If somehow there are zero rounds, default to 0 instead of crashing
        // Round the average to 2 decimal places (e.g. 67.456 becomes 67.46) before saving
        profile.setAverageScore(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));

        // Figure out which mistakes keep repeating often enough to count as a real weakness
        List<String> weaknesses = detectWeaknesses(allRounds);
        try {
            // Convert that list of weaknesses into a text (JSON) format to store in the JSONB column
            profile.setKnownWeaknesses(objectMapper.writeValueAsString(weaknesses));
        } catch (Exception e) {
            // If something breaks while converting the list to text, crash safely with a clear message
            throw new RuntimeException("Erreur lors de la sauvegarde des faiblesses détectées.", e);
        }

        // Save the updated profile (whether it's brand new or just modified) to the database
        playerProfileRepository.save(profile);
    }

    /**
     * Analyse les "dominant_mistake" des rounds récents et retient ceux qui reviennent
     * au moins WEAKNESS_MIN_OCCURRENCES fois — une faiblesse ponctuelle n'est pas un vrai motif.
     */
    private List<String> detectWeaknesses(List<GameRound> rounds) {
        // Step 1: count how many times each type of mistake appears across all rounds.
        // Example result: {"corner_blindspot": 3, "diagonal_confusion": 1}
        Map<String, Long> mistakeCounts = rounds.stream()
                .map(GameRound::getDominantMistake) // Pull just the "dominant mistake" text from each round
                .filter(Objects::nonNull) // Skip rounds where no mistake was recorded
                .filter(mistake -> !mistake.equalsIgnoreCase("none")) // Skip rounds where the AI said "no real mistake"
                .collect(Collectors.groupingBy(m -> m, Collectors.counting())); // Group identical mistakes together and count them

        // Step 2: keep only the mistakes that happened often enough to matter,
        // and turn the result back into a simple list of mistake names
        return mistakeCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= WEAKNESS_MIN_OCCURRENCES)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Fetch a player's current profile entity (used internally and by getProfileDTO)
    public PlayerProfile getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));
        return getOrCreateProfile(user);
    }

    // Converts the raw database entity into the clean DTO shape sent to Angular,
    // parsing the JSONB-stored weaknesses/strengths text back into real lists.
    public ProfileDTO getProfileDTO(String username) {
        PlayerProfile profile = getProfile(username);

        List<String> weaknesses = parseJsonList(profile.getKnownWeaknesses());
        List<String> strengths = parseJsonList(profile.getKnownStrengths());

        return new ProfileDTO(
                profile.getTotalGames() != null ? profile.getTotalGames() : 0,
                profile.getAverageScore() != null ? profile.getAverageScore() : BigDecimal.ZERO,
                weaknesses,
                strengths,
                profile.getCurrentDifficulty() != null ? profile.getCurrentDifficulty() : "BEGINNER"
        );
    }

    // HELPER: safely converts a JSON text string (or null, for a brand new profile) into a real list.
    // Never returns null — an empty list instead, so the frontend never has to check for null.
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * PILIER 3 (Half B) : génère un nouveau message de coaching personnalisé via Gemini,
     * basé sur les statistiques actuelles du joueur, puis l'enregistre dans l'historique.
     *
     * Coûte du quota Gemini — contrairement à getProfileDTO(), qui ne fait que lire des données
     * déjà en base. C'est pourquoi cette action est exposée via un POST, pas un GET.
     */
    public CoachingResponse generateCoaching(String username) {
        // Make sure the player still exists in our database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        // Reuse the existing profile-reading logic to get the player's current stats,
        // so the coaching message is always based on the SAME numbers the player sees on /me
        PlayerProfile profile = getOrCreateProfile(user);
        int totalGames = profile.getTotalGames() != null ? profile.getTotalGames() : 0;
        double averageScore = profile.getAverageScore() != null
                ? profile.getAverageScore().doubleValue()
                : 0.0;
        List<String> weaknesses = parseJsonList(profile.getKnownWeaknesses());

        // Ask Gemini to turn those stats into an actual written coaching message
        CoachingResponse coaching = aiService.generateCoachingMessage(totalGames, averageScore, weaknesses);

        // Build a short, human-readable headline for history lists (e.g. a UI showing past
        // coaching messages without needing to parse the JSON content of each one)
        String headline = (coaching.getFocusArea() == null || coaching.getFocusArea().equalsIgnoreCase("none"))
                ? "Encouragements"
                : "Focus : " + coaching.getFocusArea();

        // Save a permanent record of this coaching message, separate from the profile itself —
        // the profile holds the CURRENT snapshot, while coaching_history keeps every message ever generated
        CoachingHistory history = new CoachingHistory();
        history.setUser(user);
        history.setHeadline(headline);
        try {
            // Store the full structured response (message + focusArea) as JSON text in the jsonb column
            history.setContent(objectMapper.writeValueAsString(coaching));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la sauvegarde du message de coaching.", e);
        }
        // createdAt is set automatically by CoachingHistory's own @PrePersist — nothing to do here

        coachingHistoryRepository.save(history);

        // Return the fresh message straight to the controller so the player sees it immediately
        return coaching;
    }

    /**
    * PILIER 3 (Half B) : récupère l'historique complet des messages de coaching d'un joueur,
    * du plus récent au plus ancien, en parsant le JSON stocké dans chaque entrée.
    */
    public List<CoachingHistoryDTO> getCoachingHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        List<CoachingHistory> historyEntries = coachingHistoryRepository.findByUserOrderByCreatedAtDesc(user);

        // Convert each raw entity into the DTO shape, parsing its stored JSON content back
        // into the individual message/focusArea fields the frontend expects.
        return historyEntries.stream()
                .map(this::toCoachingHistoryDTO)
                .collect(Collectors.toList());
    }

    // HELPER: turns one CoachingHistory entity into a CoachingHistoryDTO, parsing the jsonb
    // "content" column back into a real CoachingResponse first.
    private CoachingHistoryDTO toCoachingHistoryDTO(CoachingHistory entry) {
        String message = "";
        String focusArea = "none";
        try {
            CoachingResponse parsed = objectMapper.readValue(entry.getContent(), CoachingResponse.class);
            message = parsed.getMessage();
            focusArea = parsed.getFocusArea();
        } catch (Exception e) {
            // If a row's content somehow can't be parsed, fall back to empty values rather
            // than letting one bad row break the entire history list.
            message = "(message indisponible)";
        }

        return new CoachingHistoryDTO(entry.getHeadline(), message, focusArea, entry.getCreatedAt());
    }
}