package com.back.global.rsData;

import com.fasterxml.jackson.annotation.JsonIgnore;

// 조회를 제외한 모든 API 응답의 공통 형식
// resultCode 예시 : "201-1" == HTTP 응답코드(201) + 세부코드(1)
public record RsData<T>(
        String resultCode,
        String msg,
        T data
) {
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }

    // "201-1" -> 201, JSON 응답에는 포함하지 않는다.
    @JsonIgnore
    public int statusCode() {
        return Integer.parseInt(resultCode.split("-", 2)[0]);
    }
}
