package com.graduationCapstone.Probe.domain.user.repository;

import com.graduationCapstone.Probe.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Github ID로 유저를 조회할 수 있어야 합니다.")
    void findByGithubId() {
        // given
        String githubId = "12345";
        User user = User.builder()
                .githubId(githubId)
                .username("testUser")
                .email("test@email.com")
                .build();

        userRepository.save(user);

        // when
        User foundUser = userRepository.findByGithubId(githubId)
                .orElse(null);

        // then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo("testUser");
    }
}