package com.graduationCapstone.Probe.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "표준 에러 응답 형식")
public record ErrorResponse(
                             @Schema(description = "HTTP 상태 코드", requiredMode = Schema.RequiredMode.REQUIRED)
                             int status,

                             @Schema(description = "내부 에러 코드", requiredMode = Schema.RequiredMode.REQUIRED)
                             String code,

                             @Schema(description = "사용자에게 보여줄 에러 메시지", requiredMode = Schema.RequiredMode.REQUIRED)
                             String message,

                             @Schema(description = "에러 발생 시간")
                             long timestamp,

                             @Schema(description = "필드 유효성 검사 실패 목록")
                             String errors
) {

    // CustomException 처리
    public ErrorResponse(int status, String code, String message) {
        this(status, code, message, System.currentTimeMillis(), null);
    }

    // 일반적인 예외 처리
    public ErrorResponse(HttpStatus httpStatus, String message) {
        this(httpStatus.value(), String.valueOf(httpStatus.value()), message, System.currentTimeMillis(), null);
    }
}