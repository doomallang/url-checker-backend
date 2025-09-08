package com.doomole.uptime.config;

import com.doomole.uptime.exception.ClientErrorException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionhandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionhandler.class);

    public ProblemDetail handleClientError(Exception ex, HttpServletRequest request) {
        // 상세 로그는 warn (스택트레이스는 필요 시만)
        log.warn("CLIENT_ERROR {} {} - {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setDetail(messageForClient(ex));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("category", "CLIENT_ERROR");

        // 필드 에러가 있으면 함께 내려주기(선택)
        if (ex instanceof MethodArgumentNotValidException manv) {
            var errors = manv.getBindingResult().getFieldErrors().stream()
                    .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
                    .toList();
            pd.setProperty("errors", errors);
        }
        return pd;
    }

    // === 2) SERVER_ERROR: 나머지는 전부 500 ===
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleServerError(Exception ex, HttpServletRequest req) {
        String rid = UUID.randomUUID().toString(); // 추적용
        log.error("SERVER_ERROR {} {} [rid={}]", req.getMethod(), req.getRequestURI(), rid, ex);

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setDetail("서버에서 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("category", "SERVER_ERROR");
        return pd;
    }

    private String messageForClient(Exception ex) {
        // 과도한 내부 메시지는 숨기고, 사용자에게 필요한 범위만 노출
        if (ex instanceof ClientErrorException) return ex.getMessage();
        if (ex instanceof MethodArgumentNotValidException) return "요청 본문에 유효하지 않은 값이 있습니다.";
        if (ex instanceof ConstraintViolationException) return "요청 파라미터가 유효하지 않습니다.";
        if (ex instanceof MissingServletRequestParameterException) return "필수 파라미터가 누락되었습니다.";
        if (ex instanceof HttpMessageNotReadableException) return "요청 형식이 올바르지 않습니다.";
        if (ex instanceof IllegalArgumentException) return ex.getMessage();
        return "요청이 올바르지 않습니다.";
    }
}
