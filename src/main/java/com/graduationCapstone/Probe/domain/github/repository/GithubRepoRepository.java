package com.graduationCapstone.Probe.domain.github.repository;

import com.graduationCapstone.Probe.domain.github.entity.GithubRepo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GithubRepoRepository extends JpaRepository<GithubRepo, Long> {
    List<GithubRepo> findByUserId(Long userId);
    Optional<GithubRepo> findByUserIdAndName(Long userId, String name);
}
