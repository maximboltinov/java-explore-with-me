package ru.practicum.ewm.mainservice.exception.custom;

public class IncorrectParametersException extends RuntimeException {
    public IncorrectParametersException(String message) {
        super(message);
    }
}
