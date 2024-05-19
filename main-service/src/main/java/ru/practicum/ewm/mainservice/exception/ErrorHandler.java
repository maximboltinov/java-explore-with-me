package ru.practicum.ewm.mainservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;

import javax.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.Collections;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handlerValidation(final MethodArgumentNotValidException e) {
        log.info("Завершен ошибкой", e);

        return ApiError.builder()
                .errors(Collections.singletonList(e.getAllErrors()))
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Некорректно составлен запрос")
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handlerConstraintViolation(final ConstraintViolationException e) {
        log.info("Завершен ошибкой", e);

        return ApiError.builder()
                .errors(Arrays.asList(e.getStackTrace()))
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Некорректно составлен запрос")
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handlerIntegrity(final DataIntegrityViolationException e) {
        log.info("Завершен ошибкой", e);

        return ApiError.builder()
                .errors(Arrays.asList(e.getStackTrace()))
                .status(HttpStatus.CONFLICT.name())
                .reason("Нарушение целостности данных")
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(ObjectNotFoundExceptionCust.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handlerObjectNotFoundCust(final ObjectNotFoundExceptionCust e) {
        log.info("Завершен ошибкой", e);

        return ApiError.builder()
                .errors(Arrays.asList(e.getStackTrace()))
                .status(HttpStatus.NOT_FOUND.name())
                .reason("Искомый объект не был найден")
                .message(e.getMessage())
                .build();
    }
}
