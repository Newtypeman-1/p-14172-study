package com.back.global.globalExceptionHandler;

import com.back.global.rsData.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

// 모든 컨트롤러에서 던져진 예외를 한 곳에서 처리한다.
@RestControllerAdvice
public class GlobalExceptionHandler {
    // Optional.get() 했는데 값이 없을 때 == 없는 데이터를 요청했을 때
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RsData<Void>> handle(NoSuchElementException ex) {
        return ResponseEntity
                .status(NOT_FOUND)
                .body(new RsData<>("404-1", "해당 데이터가 존재하지 않습니다."));
    }
}
