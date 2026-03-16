package com.graduationCapstone.Probe.domain.project.entity;

import com.graduationCapstone.Probe.domain.github.entity.GithubRepo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    // Lombok의 @EqualsAndHashCode 대신 직접 구현하여 안정성 확보
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectRepo that)) return false;
        // ID가 있을 때만 ID로 비교, ID가 없는 신규 객체는 무조건 다른 객체로 취급
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return 31; // 모든 객체가 동일한 해시값을 갖게 하여 HashSet이 equals()를 강제 호출하게 함
    }

    public void setProject(Project project) {
        if (this.project != null) {
            this.project.getProjectRepos().remove(this);
        }

        this.project = project;

        if (project != null) {
            project.getProjectRepos().add(this);
        }
    }
}