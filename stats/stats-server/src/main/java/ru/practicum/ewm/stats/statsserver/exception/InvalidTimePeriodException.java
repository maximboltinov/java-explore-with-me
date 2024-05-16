package ru.practicum.ewm.stats.statsserver.exception;

public class InvalidTimePeriodException extends RuntimeException {
    public InvalidTimePeriodException(String massage) {
        super(massage);
    }
}
