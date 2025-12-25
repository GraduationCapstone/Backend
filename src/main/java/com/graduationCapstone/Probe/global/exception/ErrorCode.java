package com.graduationCapstone.Probe.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 BAD REQUEST (잘못된 요청 또는 유효성 검사 실패)
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "INVALID400", "잘못된 요청 인자입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID401", "유효하지 않은 입력 값입니다."),
    USER_ROLE_EXCEPTION(HttpStatus.BAD_REQUEST, "USER40001", "존재하지 않는 인가 권한입니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH400", "지원하지 않는 OAuth 제공자입니다."),

    // 401 UNAUTHORIZED (인증 실패)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH40101", "토큰이 유효하지 않거나 만료되었습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "AUTH40102", "인증 정보가 제공되지 않았습니다."),

    // 403 FORBIDDEN (인가 실패 / 접근 권한 없음)
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH40301", "해당 리소스에 접근할 권한이 없습니다."),

    // 404 NOT FOUND (리소스를 찾지 못함)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER40401", "회원을 찾지 못했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE404", "요청한 리소스를 찾을 수 없습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "RFTOKEN404", "REFRESHTOKEN이 존재하지 않습니다."),

    // 405 METHOD NOT ALLOWED (지원하지 않는 HTTP 메서드)
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD405", "지원하지 않는 HTTP 메서드입니다."),

    // 409 CONFLICT (데이터 충돌)
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "RESOURCE409", "데이터 무결성 제약 조건(예: 중복 값)을 위반했습니다."),

    // 500 INTERNAL SERVER ERROR (서버 내부 오류)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER500", "서버 내부 오류가 발생했습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}