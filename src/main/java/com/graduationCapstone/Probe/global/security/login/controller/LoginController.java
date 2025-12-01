package com.graduationCapstone.Probe.global.security.login.controller;

import com.graduationCapstone.Probe.global.security.login.dto.TokenRequestDto;
import com.graduationCapstone.Probe.global.security.login.dto.TokenResponseDto;
import com.graduationCapstone.Probe.global.security.login.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "인증 관리 (Auth)", description = "JWT 토큰 재발급 및 로그아웃 처리")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @Operation(summary = "Access Token 재발급", description = "만료된 Access Token을 Refresh Token을 통해 새롭게 발급 받습니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "새로운 Access Token 및 Refresh Token 발급 성공",
                    content = @Content(schema = @Schema(implementation = TokenResponseDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "Refresh Token이 유효하지 않거나 만료됨"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponseDto> reissue(@RequestBody TokenRequestDto request) {
        return ResponseEntity.ok(loginService.reissue(request.refreshToken()));
    }


    @Operation(summary = "로그아웃", description = "Refresh Token을 DB에서 삭제하여 토큰 재발급 경로를 차단합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃(Refresh Token 삭제) 성공"),
            @ApiResponse(responseCode = "401", description = "Access Token이 유효하지 않거나 누락됨")
    })
    @SecurityRequirement(name = "BearerAuth") // JWT 인증 필요
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String accessToken
    ) {
        String token = accessToken.substring(7);
        loginService.logout(token);
        return ResponseEntity.ok().build();
    }
}
