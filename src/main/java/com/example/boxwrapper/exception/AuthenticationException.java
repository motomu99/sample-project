package com.example.boxwrapper.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 認証失敗時にスローされる例外
 */
@Getter
public class AuthenticationException extends RuntimeException {

    private final HttpStatus httpStatus = HttpStatus.UNAUTHORIZED;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
