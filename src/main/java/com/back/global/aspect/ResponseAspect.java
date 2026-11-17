package com.back.global.aspect;

import com.back.global.rsData.RsData;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

// 컨트롤러 액션 메서드가 RsData 를 리턴하면,
// resultCode 의 앞자리를 보고 HTTP 응답코드를 맞춰준다. (예 : "201-1" -> 201)
@Aspect
@Component
@RequiredArgsConstructor
public class ResponseAspect {
    private final HttpServletResponse response;

    @Around("""
            execution(public com.back.global.rsData.RsData *(..)) &&
            within(@org.springframework.web.bind.annotation.RestController *)
            """)
    public Object handleResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed = joinPoint.proceed();

        RsData<?> rsData = (RsData<?>) proceed;
        response.setStatus(rsData.statusCode());

        return proceed;
    }
}
