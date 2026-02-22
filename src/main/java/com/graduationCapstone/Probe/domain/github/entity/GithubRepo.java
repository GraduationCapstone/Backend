package com.graduationCapstone.Probe.domain.github.entity;

import com.graduationCapstone.Probe.domain.github.dto.GithubRepoSummaryDto;
import com.graduationCapstone.Probe.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Table(uniqueConstraints = {@UniqueConstraint(name = "uk_user_repo", columnNames = {"user_id", "repo_name"})})
public class GithubRepo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_name", nullable = false)
    private String repoName;

    @Column(name = "repo_owner", nullable = false)
    private String owner;

    @Column(name = "repo_description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "repo_language")
    private String language;

    @Column(name = "repo_forks_count")
    private int forksCount;

    @Column(name = "repo_stargazers_count")
    private int stargazersCount;

    @Column(name = "repo_open_issues_count")
    private int openIssuesCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public void updateFromSummary(GithubRepoSummaryDto dto) {
        this.owner = dto.owner();
        this.description = dto.description();
        this.language = dto.language();
        this.forksCount = dto.forksCount();
        this.stargazersCount = dto.stargazersCount();
        this.openIssuesCount = dto.openIssuesCount();
    }
}
