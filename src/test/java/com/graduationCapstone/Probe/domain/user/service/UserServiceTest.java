package com.graduationCapstone.Probe.domain.user.service;

import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.repository.UserRepository;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.security.login.repository.RefreshTokenRepository;
import com.graduationCapstone.Probe.global.security.oauth.dto.OAuth2ResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    // 테스트용 DTO
    private OAuth2ResponseDto createDto(String githubId, String username, String email) {
        return new OAuth2ResponseDto(
                Collections.emptyMap(),
                "id",
                githubId,
                username,
                email
        );
    }

    // 테스트용 User Entity
    private User createUser(String githubId, String username, String email, boolean isDeleted) {
        return User.builder()
                .githubId(githubId)
                .username(username)
                .email(email)
                .deleted(isDeleted)
                .build();
    }

    @Test
    @DisplayName("신규 회원 저장: DB에 없는 유저라면 toEntity()로 생성하여 저장합니다.")
    void saveOrUpdateUser_NewUser() {
        // given
        String githubId = "new-github-id";
        OAuth2ResponseDto dto = createDto(githubId, "new-user", "new@test.com");

        // DB에 해당 githubId가 없음
        given(userRepository.findByGithubId(githubId)).willReturn(Optional.empty());

        // when
        userService.saveOrUpdateUser(dto);

        // then
        // User 객체가 새로 생성되어 save 메서드에 전달되었는지 확인
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("기존 회원 업데이트: 이미 존재하면 updateUsername()을 호출해 닉네임을 변경합니다.")
    void saveOrUpdateUser_ExistingUser() {
        // given
        String githubId = "existing-id";
        String oldName = "old-name";
        String newName = "updated-name";

        OAuth2ResponseDto dto = createDto(githubId, newName, "email@test.com");

        // 기존 유저 (deleted = false)
        User existingUser = createUser(githubId, oldName, "email@test.com", false);
        given(userRepository.findByGithubId(githubId)).willReturn(Optional.of(existingUser));

        // when
        userService.saveOrUpdateUser(dto);

        // then
        // 객체의 username이 변경되었는지 확인 (updateUsername 동작 검증)
        assertThat(existingUser.getUsername()).isEqualTo(newName);
        // 탈퇴 상태가 아닌지 확인
        assertThat(existingUser.isDeleted()).isFalse();
        // save 호출 확인
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("탈퇴 회원 복구: 탈퇴 상태라면 reactivate()를 호출하고 닉네임을 업데이트합니다.")
    void saveOrUpdateUser_ReactivateDeletedUser() {
        // given
        String githubId = "deleted-id";
        String newName = "return-user";
        OAuth2ResponseDto dto = createDto(githubId, newName, "email@test.com");

        // 기존 유저 (deleted = true)
        User deletedUser = createUser(githubId, "old-name", "email@test.com", true);

        // 실행 전 상태 확인
        assertThat(deletedUser.isDeleted()).isTrue();

        given(userRepository.findByGithubId(githubId)).willReturn(Optional.of(deletedUser));

        // when
        userService.saveOrUpdateUser(dto);

        // then
        // reactivate()가 호출되어 deleted가 false로 변했는지 확인
        assertThat(deletedUser.isDeleted()).isFalse();
        // 닉네임 업데이트 확인
        assertThat(deletedUser.getUsername()).isEqualTo(newName);
        // save 호출 확인
        verify(userRepository).save(deletedUser);
    }

    @Test
    @DisplayName("회원 탈퇴 성공: 토큰을 삭제하고 deleted(true)를 호출합니다.")
    void deleteUser_Success() {
        // given
        Long userId = 1L;
        // 활성 상태 유저 생성
        User user = createUser("gh-id", "user", "email", false);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.deleteUser(userId);

        // then
        // 리프레시 토큰 삭제 확인
        verify(refreshTokenRepository).deleteByUserId(userId);

        // 유저 객체의 상태가 deleted=true로 변경되었는지 확인
        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("회원 탈퇴 실패: 유저가 없으면 USER_NOT_FOUND 예외가 발생합니다.")
    void deleteUser_Fail_NotFound() {
        // given
        Long userId = 999L;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
}