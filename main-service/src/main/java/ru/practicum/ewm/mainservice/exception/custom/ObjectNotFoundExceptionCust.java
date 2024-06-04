package ru.practicum.ewm.mainservice.exception.custom;

public class ObjectNotFoundExceptionCust extends RuntimeException {
    public ObjectNotFoundExceptionCust(String message) {
        super(message);
    }
}
