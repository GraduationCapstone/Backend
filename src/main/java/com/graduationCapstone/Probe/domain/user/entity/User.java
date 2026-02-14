package com.graduationCapstone.Probe.domain.user.entity;

import com.graduationCapstone.Probe.domain.project.entity.ProjectMember;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("숫자로 구성된 회원의 GitHub 고유 id")
    @Column(name ="github_id", nullable = false)
    private String githubId;

    @Comment("회원의 GitHub 닉네임")
    @Column(name = "username", nullable = false)
    private String username;

    @Comment("회원의 GitHub 이메일 주소")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Comment("회원의 회원 탈퇴 여부")
    @Column(name = "deleted", columnDefinition = "boolean default false")
    private boolean deleted;

    @Comment("회원의 Github 접근을 위한 AccessToken")
    @Column(name = "github_access_token")
    private String githubAccessToken;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectMember> projectMembers = new ArrayList<>();

    public void deleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void reactivate() {
        this.deleted = false;
    }

    public void updateUsername(String username) {
        this.username = username;
    }

    public void updateGithubAccessToken(String token) {
        this.githubAccessToken = token;
    }
}
