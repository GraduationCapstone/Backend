package com.graduationCapstone.Probe.domain.project.entity;

import com.graduationCapstone.Probe.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Table(name = "project_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "user_id"})
})
public class ProjectMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role;

    public void setProject(Project project) {
        if (this.project != null) {
            this.project.getMembers().remove(this);
        }

        this.project = project;

        if (project != null) {
            project.getMembers().add(this);
        }
    }

    public void setUser(User user) {
        if (this.user == user) {
            return;
        }

        this.user = user;

        if (user != null) {
            user.addProjectMember(this);
        }
    }
}