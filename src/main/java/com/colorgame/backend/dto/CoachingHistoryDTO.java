// This tells the computer where this file lives in the project's folder structure
package com.colorgame.backend.dto;

import java.time.LocalDateTime;

// Shape of a single coaching history entry sent to Angular — combines the entity's own
// fields (headline, createdAt) with the parsed content (message, focusArea) so the
// frontend never has to parse JSON itself.
public class CoachingHistoryDTO {

    private String headline;
    private String message;
    private String focusArea;
    private LocalDateTime createdAt;

    public CoachingHistoryDTO() {}

    public CoachingHistoryDTO(String headline, String message, String focusArea, LocalDateTime createdAt) {
        this.headline = headline;
        this.message = message;
        this.focusArea = focusArea;
        this.createdAt = createdAt;
    }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFocusArea() { return focusArea; }
    public void setFocusArea(String focusArea) { this.focusArea = focusArea; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}