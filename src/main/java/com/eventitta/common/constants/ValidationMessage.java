package com.eventitta.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationMessage {
    public static final String EMAIL = "올바른 이메일 형식이어야 합니다.";
    public static final String PASSWORD = "비밀번호는 8~20자이며, 영문, 숫자, 특수문자를 모두 포함해야 합니다.";
    public static final String NICKNAME = "닉네임은 2자 이상 20자 이하로 입력해주세요.";
    public static final String TITLE = "게시글 제목을 입력해주세요.";
    public static final String CONTENT = "게시글 내용을 입력해주세요.";
    public static final String REGION_CODE = "지역을 입력해주세요.";
    public static final String SEARCH_TYPE = "검색 타입을 지정해주세요.";
    public static final String KEYWORD = "검색어를 입력해주세요.";
    public static final String PAGE_MIN = "페이지 번호는 0 이상이어야 합니다.";
    public static final String SIZE_MIN = "페이지 크기는 최소 1 이상이어야 합니다.";
    public static final String SIZE_MAX = "페이지 크기는 최대 100 이하여야 합니다.";
}
