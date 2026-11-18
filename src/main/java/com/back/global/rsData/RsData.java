package com.back.global.rsData;

import com.fasterxml.jackson.annotation.JsonIgnore;

// 조회 성공을 제외한 모든 응답의 공통 형식
// resultCode : "HTTP 응답코드-세부코드" (예 : "201-1")
public record RsData<T>(
        String resultCode,
        String msg,
        T data
) {
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }

    // "201-1" -> 201
    // @JsonIgnore : 메서드 이름이 필드처럼 보여도 JSON 응답에는 넣지 않는다.
    @JsonIgnore
    public int statusCode() {
        return Integer.parseInt(resultCode.split("-", 2)[0]);
    }
}
