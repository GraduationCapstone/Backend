package com.graduationCapstone.Probe.domain.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "test_group")
@SQLDelete(sql = "UPDATE test_group SET deleted_at = NOW() WHERE group_id = ?") // delete() 호출 시 자동으로 deletedAt을 업데이트하다록 설정
@Where(clause = "deleted_at IS NULL")
public class TestGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName; // 입력한 테스트그룹명 (논리적 테이블 키)

    @Column(name = "target_repo_id")
    private Long targetRepoId;

    @Column(name = "target_branch", length = 100)
    private String targetBranch;

    @Column(name = "optional_server_url")
    private String optionalServerUrl; // 사용자가 선택적으로 입력할 수 있는 서버 URL

    @OneToMany(mappedBy = "testGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestExecution> testExecutions;

    /** null이면 활성, 값이 있으면 삭제된 레코드 */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 테스트그룹명 수정 */
    public void updateGroupName(String name) {
        this.groupName = name;
    }

    /** Soft Delete 처리 */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();

        // 부모 삭제 시 자식들도 명시적으로 softDelete 호출
        if (this.testExecutions != null) {
            this.testExecutions.forEach(TestExecution::softDelete);
        }
    }

    /** 삭제 여부 확인 */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
