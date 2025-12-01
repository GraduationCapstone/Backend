package com.graduationCapstone.Probe.domain.user.controller;

import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@Tag(name = "사용자 관리 (User)", description = "로그인된 사용자의 정보 조회 및 회원탈퇴")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 현재 로그인된 사용자 정보 조회 API
     */
    @Operation(summary = "내 정보 조회", description = "유효한 Access Token으로 현재 로그인된 사용자(User)의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = User.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (유효하지 않거나 만료된 토큰)",
                    content = @Content
            )
    })
    @GetMapping("/me")
    public ResponseEntity<User> get(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(user);
    }

    /**
     * 회원 탈퇴 (논리적 삭제) API
     */
    @Operation(summary = "회원 탈퇴 (논리적 삭제)", description = "로그인된 사용자 계정을 논리적으로 삭제하고 Refresh Token을 무효화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공적으로 탈퇴 처리됨 (No Content)"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않거나 만료된 토큰)"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음 (논리적 삭제 과정 중)")
    })
    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user) {
        Long userId = user.getId();
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
