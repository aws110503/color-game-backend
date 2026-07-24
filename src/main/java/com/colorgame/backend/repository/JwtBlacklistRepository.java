package com.colorgame.backend.repository;

import com.colorgame.backend.model.JwtBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JwtBlacklistRepository extends JpaRepository<JwtBlacklist, Long> {

    boolean existsByJti(String jti);

    Optional<JwtBlacklist> findByJti(String jti);
}
