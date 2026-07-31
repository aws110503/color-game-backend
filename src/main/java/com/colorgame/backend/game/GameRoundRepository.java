package com.colorgame.backend.game;

import com.colorgame.backend.model.GameRound;
import com.colorgame.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRoundRepository extends JpaRepository<GameRound, Long> {

    List<GameRound> findByUserOrderByPlayedAtDesc(User user);

    List<GameRound> findTop5ByUserOrderByPlayedAtDesc(User user);

    Optional<GameRound> findByIdAndUser(Long id, User user);
}