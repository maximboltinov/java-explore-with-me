package ru.practicum.ewm.mainservice.service.implementation;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.mainservice.dto.user.UserDto;
import ru.practicum.ewm.mainservice.dto.user.NewUserRequest;
import ru.practicum.ewm.mainservice.mapper.UserMapper;
import ru.practicum.ewm.mainservice.model.User;
import ru.practicum.ewm.mainservice.repository.JpaUserRepository;
import ru.practicum.ewm.mainservice.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final JpaUserRepository jpaUserRepository;

    @Override
    public UserDto create(NewUserRequest newUserRequest) {
        User user = jpaUserRepository.save(UserMapper.newUserRequestToUser(newUserRequest));
        return UserMapper.userToUserDto(user);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        Pageable page = PageRequest.of(from / size, size);

        if (ids == null || ids.isEmpty()) {
            return jpaUserRepository.findAll(page).stream()
                    .map(UserMapper::userToUserDto)
                    .collect(Collectors.toList());
        } else {
            return jpaUserRepository.getByIdIn(ids, page).stream()
                    .map(UserMapper::userToUserDto)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void deleteUser(Long userId) {
        jpaUserRepository.deleteById(userId);
    }
}
