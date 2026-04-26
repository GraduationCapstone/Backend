package com.graduationCapstone.Probe.domain.project.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "github_repository")
public class GithubRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repo_id")
    private Long repoId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    // 원본 GithubRepo 엔티티의 ID
    @Column(name = "github_repo_id", nullable = false)
    private Long githubRepoId;

    @Column(name = "repo_url", nullable = false)
    private String repoUrl;

    // PUBLIC, PRIVATE
    @Column(name = "visibility", nullable = false, length = 10)
    @ColumnDefault("'PUBLIC'")
    private String visibility;

    @Column(name = "primary_language", length = 25)
    private String primaryLanguage;

    @Column(name = "star_count", nullable = false)
    @ColumnDefault("0")
    private Integer starCount;

    @Column(name = "fork_count", nullable = false)
    @ColumnDefault("0")
    private Integer forkCount;

    @Column(name = "github_updated_at")
    private LocalDateTime githubUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
