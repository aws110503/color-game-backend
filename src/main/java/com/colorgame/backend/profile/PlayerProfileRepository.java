package com.colorgame.backend.profile;

import com.colorgame.backend.model.PlayerProfile;
import com.colorgame.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {
    Optional<PlayerProfile> findByUser(User user);
}