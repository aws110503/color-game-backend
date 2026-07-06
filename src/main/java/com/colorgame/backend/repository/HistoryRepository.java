package com.colorgame.backend.repository;

import com.colorgame.backend.model.GridHistory;
import com.colorgame.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryRepository extends JpaRepository<GridHistory, Long> {

    List<GridHistory> findByUser(User user);

    List<GridHistory> findByUserOrderByChangedAtDesc(User user);

    List<GridHistory> findAllByOrderByChangedAtDesc();
}