package com.graduationCapstone.Probe.domain.test.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "test_group")
public class TestGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;
    private Long projectId;
    private String groupName; // 입력한 테스트그룹명 (논리적 테이블 키)
    private Long targetRepoId;
    private String targetBranch;

    @OneToMany(mappedBy = "testGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestExecution> testExecutions;

    public void updateGroupName(String name) { this.groupName = name; }
}
