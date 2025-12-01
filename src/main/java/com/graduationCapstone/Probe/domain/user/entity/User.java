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

    public void deleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void reactivate() {
        this.deleted = false;
    }

    public void updateUsername(String username) {
        this.username = username;
    }
}
