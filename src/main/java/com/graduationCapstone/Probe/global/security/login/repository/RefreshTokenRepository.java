package com.graduationCapstone.Probe.global.security.login.repository;

import com.graduationCapstone.Probe.global.security.login.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    void deleteByUserId(Long userId);

    Optional<RefreshToken> findByUserId(Long userId);

}
