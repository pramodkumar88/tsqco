package com.tsqco.exception;

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.util.Date;

@ControllerAdvice
public class TsqcoGlobalExceptionHandler {

    @ExceptionHandler(value = {KiteException.class, IOException.class})
    public ResponseEntity<ErrorMessage> kiteConnectionException(KiteException ex, WebRequest request) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                new Date(),
                ex.getLocalizedMessage(),
                request.getDescription(true));
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    }
}
