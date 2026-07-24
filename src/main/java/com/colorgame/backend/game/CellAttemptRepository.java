package com.colorgame.backend.game;

import com.colorgame.backend.model.CellAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CellAttemptRepository extends JpaRepository<CellAttempt, Long> {
}