package com.back.global.aspect;

import com.back.global.rsData.RsData;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

// resultCode 를 "201-1" 로 정해도 HTTP 응답코드는 자동으로 201 이 되지 않는다.
// 그래서 @RestController 의 메서드가 RsData 를 리턴하면,
// 리턴 직후에 끼어들어서(AOP) resultCode 앞자리로 HTTP 응답코드를 바꿔준다.
@Aspect
@Component
@RequiredArgsConstructor
public class ResponseAspect {
    private final HttpServletResponse response; // 현재 요청의 응답 객체(프록시)

    @Around("""
            execution(public com.back.global.rsData.RsData *(..)) &&
            within(@org.springframework.web.bind.annotation.RestController *)
            """)
    public Object handleResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = joinPoint.proceed(); // 원래 액션 메서드 실행

        RsData<?> rsData = (RsData<?>) proceed;
        response.setStatus(rsData.statusCode());

        return proceed;
    }
}
