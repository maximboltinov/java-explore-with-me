package ru.practicum.ewm.mainservice.service;

import ru.practicum.ewm.mainservice.dto.user.NewUserRequest;
import ru.practicum.ewm.mainservice.dto.user.UserDto;
import ru.practicum.ewm.mainservice.model.User;

import java.util.List;

public interface UserService {
    UserDto create(NewUserRequest newUserRequest);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long userId);

    User checkUserById(Long userId);
}
