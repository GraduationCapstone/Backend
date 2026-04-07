package com.graduationCapstone.Probe.domain.test.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 시나리오 가이드 → 2자리 시리얼 번호 매핑.
 *
 * <p>SCENARIO_SERIAL은 테스트 시나리오 ID의 구성 요소로,
 * T[SCENARIO_SERIAL][SCENARIO_ATTEMPT] 형식의 ID를 생성하는 데 사용됩니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum ScenarioSerial {

    SIGN_UP("01", "회원관련", "회원가입"),
    LOGIN("02", "회원관련", "로그인"),
    PASSWORD_RECOVERY("03", "회원관련", "비밀번호 찾기"),
    LOGOUT("04", "회원관련", "로그아웃"),

    PROFILE_EDIT("05", "사용자 정보/권한 관련", "프로필 수정"),
    PASSWORD_CHANGE("06", "사용자 정보/권한 관련", "비밀번호 변경"),
    ROLE_BASED_ACCESS("07", "사용자 정보/권한 관련", "권한 기반 접근 제어"),
    SESSION_TOKEN_EXPIRY("08", "사용자 정보/권한 관련", "세션 만료/토큰 만료"),

    POST_CREATE("09", "게시판/콘텐츠 관련", "게시글 작성"),
    POST_EDIT_DELETE("10", "게시판/콘텐츠 관련", "게시글 수정/삭제"),
    COMMENT_CRUD("11", "게시판/콘텐츠 관련", "댓글 작성/수정/삭제"),
    LIKE_BOOKMARK("12", "게시판/콘텐츠 관련", "좋아요/즐겨찾기"),
    SEARCH("13", "게시판/콘텐츠 관련", "검색"),
    FILTER_SORT("14", "게시판/콘텐츠 관련", "필터/정렬"),

    RESPONSIVE_LAYOUT("15", "UI/UX/반응형/브라우저", "반응형 레이아웃"),
    BROWSER_COMPAT("16", "UI/UX/반응형/브라우저", "브라우저 호환성"),
    ERROR_PAGE("17", "UI/UX/반응형/브라우저", "404/500 에러 페이지 동작"),

    NETWORK_DISCONNECT("18", "네트워크/예외 상황", "네트워크 끊김 상태"),
    SERVER_LATENCY("19", "네트워크/예외 상황", "서버 응답 지연"),
    API_ERROR_HANDLING("20", "네트워크/예외 상황", "API 에러 응답 처리"),

    AB_TEST("21", "그 외", "A/B 테스트 요소 확인"),
    INPUT_VALIDATION("22", "그 외", "입력값 유효성 검사"),
    LANGUAGE_CHANGE("23", "그 외", "다국어 지원 시 언어 변경 테스트"),
    FILE_UPLOAD_DOWNLOAD("24", "그 외", "파일 업로드/다운로드"),
    PUSH_NOTIFICATION("25", "그 외", "푸시 알림"),
    CONCURRENT_ACCESS("26", "그 외", "다중 사용자 동시 접속");

    private final String code;
    private final String category;
    private final String testItem;

    /** testItem(시나리오명) → ScenarioSerial 빠른 조회용 맵 */
    private static final Map<String, ScenarioSerial> BY_TEST_ITEM =
            Stream.of(values())
                    .collect(Collectors.toUnmodifiableMap(ScenarioSerial::getTestItem, s -> s));

    /** code(시리얼 번호) → ScenarioSerial 빠른 조회용 맵 */
    private static final Map<String, ScenarioSerial> BY_CODE =
            Stream.of(values())
                    .collect(Collectors.toUnmodifiableMap(ScenarioSerial::getCode, s -> s));

    /**
     * 시나리오 가이드 이름(testItem)으로 ScenarioSerial을 조회합니다.
     *
     * @param testItem 시나리오 가이드 이름 (예: "회원가입")
     * @return 매칭되는 ScenarioSerial
     * @throws IllegalArgumentException 매핑되지 않는 testItem인 경우
     */
    public static ScenarioSerial fromTestItem(String testItem) {
        ScenarioSerial serial = BY_TEST_ITEM.get(testItem);
        if (serial == null) {
            throw new IllegalArgumentException("Unknown scenario test item: " + testItem);
        }
        return serial;
    }

    /**
     * 시리얼 코드(2자리 문자열)로 ScenarioSerial을 조회합니다.
     *
     * @param code 시리얼 코드 (예: "01")
     * @return 매칭되는 ScenarioSerial
     * @throws IllegalArgumentException 매핑되지 않는 코드인 경우
     */
    public static ScenarioSerial fromCode(String code) {
        ScenarioSerial serial = BY_CODE.get(code);
        if (serial == null) {
            throw new IllegalArgumentException("Unknown scenario serial code: " + code);
        }
        return serial;
    }
}
