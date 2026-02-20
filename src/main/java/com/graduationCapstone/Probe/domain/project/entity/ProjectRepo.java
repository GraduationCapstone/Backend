package com.graduationCapstone.Probe.domain.project.entity;

import com.graduationCapstone.Probe.domain.github.entity.GithubRepo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Table(name = "project_repo", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "github_repo_id"})
})
public class ProjectRepo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "github_repo_id", nullable = false)
    private GithubRepo githubRepo;

    public void setProject(Project project) {
        if (this.project != null) {
            this.project.getProjectRepos().remove(this);
        }

        this.project = project;

        if (project != null && !project.getProjectRepos().contains(this)) {
            project.getProjectRepos().add(this);
        }
    }
}