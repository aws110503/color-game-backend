package com.colorgame.backend.profile;

import com.colorgame.backend.dto.CoachingHistoryDTO;
import com.colorgame.backend.dto.CoachingResponse;
import com.colorgame.backend.dto.ProfileDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileDTO> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(profileService.getProfileDTO(authentication.getName()));
    }

    @PostMapping("/coaching")
    public ResponseEntity<CoachingResponse> generateCoaching(Authentication authentication) {
        return ResponseEntity.ok(profileService.generateCoaching(authentication.getName()));
    }

    @GetMapping("/coaching/history")
    public ResponseEntity<List<CoachingHistoryDTO>> getCoachingHistory(Authentication authentication) {
        return ResponseEntity.ok(profileService.getCoachingHistory(authentication.getName()));
    }
}