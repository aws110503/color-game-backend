package com.colorgame.backend.dto;

// Shape of what Gemini sends back when asked to write a coaching message.
// Same pattern as PatternResponse / ScoringResponse — a plain data holder Jackson fills in.
public class CoachingResponse {

    private String message;     // The actual encouraging, personalized coaching text
    private String focusArea;   // Single weakness Gemini chose to focus on this time (or "none")

    public CoachingResponse() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFocusArea() { return focusArea; }
    public void setFocusArea(String focusArea) { this.focusArea = focusArea; }
}