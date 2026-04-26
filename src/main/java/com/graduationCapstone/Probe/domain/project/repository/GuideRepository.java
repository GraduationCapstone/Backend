package com.graduationCapstone.Probe.domain.project.repository;

import com.graduationCapstone.Probe.domain.project.entity.Guide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuideRepository extends JpaRepository<Guide, Long> {
    Optional<Guide> findByTestItem(String testItem);
}
