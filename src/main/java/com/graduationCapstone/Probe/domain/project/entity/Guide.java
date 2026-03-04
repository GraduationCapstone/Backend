package com.graduationCapstone.Probe.domain.project.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "guide")
public class Guide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_id")
    private Long guideId;

    // 회원 관련, 사용자 정보/권한 관련
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    // 회원가입, 로그인 등 구체적인 테스트 항목
    @Column(name = "test_item", nullable = false)
    private String testItem;
}
