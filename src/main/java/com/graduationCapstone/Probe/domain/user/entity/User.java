package com.graduationCapstone.Probe.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String githubId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(columnDefinition = "boolean default false")
    private boolean deleted;

    public void update(String username) {
        this.username = username;
    }

    public void deleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void reactivate() {
    }

    public void updateUsername(String username) {
    }
}
