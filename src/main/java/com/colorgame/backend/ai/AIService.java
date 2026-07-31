// This tells the computer where this file lives in the project's folder structure
package com.colorgame.backend.ai;

// Brings in the List tool so the coaching method can accept a list of weaknesses
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.colorgame.backend.dto.CoachingResponse;

// @Service tells the app: "This file is a Worker Bee that talks to the Gemini AI and turns
// its raw text replies into clean Java objects the rest of the app can use."
@Service
public class AIService {

    // These are placeholders for the tools this service needs to do its job
    private final GeminiClient geminiClient;       // The actual tool that sends requests to Gemini and gets text back
    private final PromptBuilder promptBuilder;     // Builds the instruction text sent to Gemini for each task
    private final ObjectMapper objectMapper = new ObjectMapper(); // Converts Gemini's JSON text into real Java objects

    // This is the "Constructor" — it hooks up all the tools mentioned above so they are ready to use
    public AIService(GeminiClient geminiClient, PromptBuilder promptBuilder) {
        this.geminiClient = geminiClient;
        this.promptBuilder = promptBuilder;
    }

    // PILIER 1 : asks Gemini to invent a new colored grid pattern for the player to memorize
    public PatternResponse generatePattern(String gridSize, String difficulty) {
        // Build the instruction text describing what grid to generate
        String prompt = promptBuilder.buildPatternGenerationPrompt(gridSize, difficulty);
        // Send it to Gemini and get back its raw text reply
        String rawResponse = geminiClient.generateContent(prompt);
        // Strip away any markdown fences Gemini might have added around the JSON
        String cleanJson = cleanJsonResponse(rawResponse);

        try {
            // Convert the cleaned JSON text into a real PatternResponse object
            return objectMapper.readValue(cleanJson, PatternResponse.class);
        } catch (Exception e) {
            // If Gemini's reply wasn't valid JSON or didn't match the expected shape, fail loudly with details
            throw new RuntimeException("Gemini a retourné un pattern invalide : " + e.getMessage(), e);
        }
    }

    // PILIER 1 : asks Gemini to grade the player's recreated grid against the original, cell by cell
    public ScoringResponse scoreAttempt(String targetGridJson, String playerGridJson) {
        // Build the instruction text describing both grids to compare
        String prompt = promptBuilder.buildScoringPrompt(targetGridJson, playerGridJson);
        // Send it to Gemini and get back its raw text reply
        String rawResponse = geminiClient.generateContent(prompt);
        // Strip away any markdown fences Gemini might have added around the JSON
        String cleanJson = cleanJsonResponse(rawResponse);

        try {
            // Convert the cleaned JSON text into a real ScoringResponse object
            return objectMapper.readValue(cleanJson, ScoringResponse.class);
        } catch (Exception e) {
            // If Gemini's reply wasn't valid JSON or didn't match the expected shape, fail loudly with details
            throw new RuntimeException("Gemini a retourné un score invalide : " + e.getMessage(), e);
        }
    }

    // PILIER 3 (Half B) : asks Gemini to turn a player's raw stats into a short, personalized,
    // encouraging coaching message. Same three-step pattern as the two methods above:
    // build prompt -> call Gemini -> clean and parse the JSON reply.
    public CoachingResponse generateCoachingMessage(int totalGames, double averageScore, List<String> weaknesses) {
        // Build the instruction text describing the player's stats and detected weaknesses
        String prompt = promptBuilder.buildCoachingPrompt(totalGames, averageScore, weaknesses);
        // Send it to Gemini and get back its raw text reply
        String rawResponse = geminiClient.generateContent(prompt);
        // Strip away any markdown fences Gemini might have added around the JSON
        String cleanJson = cleanJsonResponse(rawResponse);

        try {
            // Convert the cleaned JSON text into a real CoachingResponse object (message + focusArea)
            return objectMapper.readValue(cleanJson, CoachingResponse.class);
        } catch (Exception e) {
            // If Gemini's reply wasn't valid JSON or didn't match the expected shape, fail loudly with details
            throw new RuntimeException("Gemini a retourné un message de coaching invalide : " + e.getMessage(), e);
        }
    }

    /**
     * Gemini répond parfois avec des balises markdown (```json ... ```) malgré les instructions.
     * On les retire avant de parser, pour éviter une erreur JSON.
     *
     * HELPER: shared by all three methods above — removes any ```json / ``` fences Gemini
     * sometimes wraps around its JSON reply, since those fences would break JSON parsing.
     */
    private String cleanJsonResponse(String raw) {
        String cleaned = raw.trim(); // Remove leading/trailing whitespace first
        if (cleaned.startsWith("```")) {
            // Remove an opening ```json fence (with optional trailing whitespace/newline)
            cleaned = cleaned.replaceAll("^```json\\s*", "");
            // Remove a plain opening ``` fence, in case there was no "json" language tag
            cleaned = cleaned.replaceAll("^```\\s*", "");
            // Remove a closing ``` fence at the very end
            cleaned = cleaned.replaceAll("```\\s*$", "");
        }
        return cleaned.trim(); // Trim once more in case removing fences left stray whitespace
    }
}