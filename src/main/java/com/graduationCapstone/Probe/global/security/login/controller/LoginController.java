package com.graduationCapstone.Probe.global.security.login.controller;

import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.global.security.login.dto.TokenResponseDto;
import com.graduationCapstone.Probe.global.security.login.service.LoginService;
import com.graduationCapstone.Probe.global.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final CookieUtil cookieUtil;

    @Operation(summary = "Access Token 재발급", description = " 쿠키의 Refresh Token을 검증하여 새로운 Access/Refresh Token을 모두 쿠키로 재발급합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "토큰 재발급 성공",
                    content = @Content // Body 없음
            ),
            @ApiResponse(responseCode = "401", description = "Refresh Token이 유효하지 않거나 만료됨"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping("/reissue")
    public ResponseEntity<Void> reissue(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        loginService.reissue(refreshToken, response);

        return ResponseEntity.noContent().build();

    }


    @Operation(summary = "로그아웃", description = "Refresh Token을 DB에서 삭제하고, 브라우저에 저장된 모든 인증 관련 쿠키를 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃(Refresh Token 삭제) 성공")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        loginService.logout(request, response);

        return ResponseEntity.noContent().build();
    }
}
