package com.back.global.globalExceptionHandler;

import com.back.global.rsData.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
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

    // @Valid 검증 실패 (예 : 제목 없이 글 작성)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handle(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .filter(error -> error instanceof FieldError)
                .map(error -> (FieldError) error)
                .map(error -> error.getField() + "-" + error.getCode() + "-" + error.getDefaultMessage())
                .sorted() // 검증 순서가 매번 달라도 메시지가 같도록 정렬
                .collect(Collectors.joining("\n"));

        return ResponseEntity
                .status(BAD_REQUEST)
                .body(new RsData<>("400-1", message));
    }

    // 요청 본문을 JSON 으로 읽을 수 없을 때 (예 : 중괄호가 안 닫힌 JSON)
    // 처리하지 않으면 스프링 기본 오류 응답이 나가고, 개발 환경에서는 스택 트레이스까지 노출된다.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RsData<Void>> handle(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(new RsData<>("400-1", "요청 본문이 올바르지 않습니다."));
    }
}
