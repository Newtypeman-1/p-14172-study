package com.back.global.rsData;

// 조회 성공을 제외한 모든 응답의 공통 형식
// resultCode : "HTTP 응답코드-세부코드" (예 : "404-1")
public record RsData<T>(
        String resultCode,
        String msg,
        T data
) {
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }
}
