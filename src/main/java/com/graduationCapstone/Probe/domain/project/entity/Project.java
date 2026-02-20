package com.graduationCapstone.Probe.domain.project.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Table(name = "project")
public class Project {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectName;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ProjectRepo> projectRepos = new HashSet<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ProjectMember> members = new HashSet<>();

    public void updateProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void addMember(ProjectMember member) {
        this.members.add(member);
        if (member.getProject() != this) {
            member.setProject(this);
        }
    }

    public void addProjectRepo(ProjectRepo projectRepo) {
        this.projectRepos.add(projectRepo);
        if (projectRepo.getProject() != this) {
            projectRepo.setProject(this);
        }
    }
}