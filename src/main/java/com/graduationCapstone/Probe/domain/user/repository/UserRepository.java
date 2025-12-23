package com.graduationCapstone.Probe.domain.user.repository;

import com.graduationCapstone.Probe.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGithubIdAndDeletedFalse(String githubId);
    Optional<User> findByIdAndDeletedFalse(Long id);
    Optional<User> findByGithubId(String githubId);

}
