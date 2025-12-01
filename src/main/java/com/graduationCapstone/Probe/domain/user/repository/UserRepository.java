package com.graduationCapstone.Probe.domain.user.repository;

import com.graduationCapstone.Probe.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGithubIdAndDeletedFalse(String githubId);
    Optional<User> findByIdAndDeletedFalse(Long id);

}
