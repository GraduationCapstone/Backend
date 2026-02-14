package com.graduationCapstone.Probe.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 BAD REQUEST (잘못된 요청 또는 유효성 검사 실패)
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "INVALID40001", "잘못된 요청 인자입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID40002", "유효하지 않은 입력 값입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "INVALID40003", "유효하지 않은 리프레시 토큰입니다."),
    USER_ROLE_EXCEPTION(HttpStatus.BAD_REQUEST, "USER40001", "존재하지 않는 인가 권한입니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH40001", "지원하지 않는 OAuth 제공자입니다."),

    // 401 UNAUTHORIZED (인증 실패)
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH40101", "토큰이 유효하지 않거나 만료되었습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "AUTH40102", "인증 정보가 제공되지 않았습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH40103", "토큰의 유저 정보가 일치하지 않습니다."),

    // 403 FORBIDDEN (인가 실패 / 접근 권한 없음)
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH40301", "해당 리소스에 접근할 권한이 없습니다."),

    // 404 NOT FOUND (리소스를 찾지 못함)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER40401", "회원을 찾지 못했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE40401", "요청한 리소스를 찾을 수 없습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "RFTOKEN40401", "로그아웃 된 사용자입니다."),
    REPOSITORY_NOT_FOUND(HttpStatus.NOT_FOUND, "REPO40401", "레포지토리 데이터가 비어있습니다"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE40401", "해당 파일을 찾을 수 없거나 불러올 수 없습니다"),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJ40401", "해당 프로젝트를 찾을 수 없습니다"),

    // 405 METHOD NOT ALLOWED (지원하지 않는 HTTP 메서드)
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD40501", "지원하지 않는 HTTP 메서드입니다."),

    // 409 CONFLICT (데이터 충돌)
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "RESOURCE40901", "데이터 무결성 제약 조건(예: 중복 값)을 위반했습니다."),

    // 500 INTERNAL SERVER ERROR (서버 내부 오류)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER50001", "서버 내부 오류가 발생했습니다."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}