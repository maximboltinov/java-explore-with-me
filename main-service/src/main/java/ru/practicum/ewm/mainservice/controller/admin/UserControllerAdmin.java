package ru.practicum.ewm.mainservice.controller.admin;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.mainservice.dto.user.NewUserRequest;
import ru.practicum.ewm.mainservice.dto.user.UserDto;
import ru.practicum.ewm.mainservice.service.UserService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@RequestMapping("/admin/users")
@AllArgsConstructor
@Slf4j
@Validated
public class UserControllerAdmin {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody @Valid NewUserRequest newUserRequest) {
        log.info("Запрос POST /admin/users {}", newUserRequest);
        UserDto userDto = userService.create(newUserRequest);
        log.info("Ответ POST /admin/users {}", userDto);
        return userDto;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                  @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("Запрос GET /admin/users ids {} from {} size {}", ids, from, size);
        List<UserDto> userDtoList = userService.getUsers(ids, from, size);
        log.info("Ответ GET /admin/users ids {} from {} size {} answer {}", ids, from, size, userDtoList);
        return userDtoList;
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable @Positive Long userId) {
        log.info("Запрос DELETE /admin/users{}", userId);
        userService.deleteUser(userId);
        log.info("Ответ DELETE /admin/users{} {}", userId, HttpStatus.NO_CONTENT);
    }
}
