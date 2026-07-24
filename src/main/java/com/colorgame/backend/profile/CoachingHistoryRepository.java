package com.colorgame.backend.profile;

import com.colorgame.backend.model.CoachingHistory;
import com.colorgame.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachingHistoryRepository extends JpaRepository<CoachingHistory, Long> {
    List<CoachingHistory> findByUserOrderByCreatedAtDesc(User user);
}