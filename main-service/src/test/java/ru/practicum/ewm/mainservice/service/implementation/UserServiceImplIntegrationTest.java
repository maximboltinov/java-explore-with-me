package ru.practicum.ewm.mainservice.service.implementation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import ru.practicum.ewm.mainservice.dto.user.NewUserRequest;
import ru.practicum.ewm.mainservice.dto.user.UserDto;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.repository.JpaUserRepository;
import ru.practicum.ewm.mainservice.service.UserService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceImplIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private JpaUserRepository userRepository;

    @Test
    void create_correct() {
        NewUserRequest newUserRequest = new NewUserRequest("mail@mail.com", "John Tyotkin");

        UserDto userDto = userService.create(newUserRequest);
        List<UserDto> users = userService.getUsers(null, 0, 10);

        assertEquals(1, users.size());
        assertEquals("mail@mail.com", userDto.getEmail());
        assertEquals("John Tyotkin", userDto.getName());
        assertNotNull(userDto.getId());
    }

    @Test
    void create_userWithBusyEmailAddress_DataIntegrityViolationException() {
        NewUserRequest newUserRequest = new NewUserRequest("mail@mail.com", "John Tyotkin");

        userService.create(newUserRequest);

        assertThrows(DataIntegrityViolationException.class, () -> userService.create(newUserRequest));
    }

    @Test
    void getUsers_correct() {
        NewUserRequest newUserRequest1 = new NewUserRequest("mail1@mail.com", "John Tyotkin");
        NewUserRequest newUserRequest2 = new NewUserRequest("mail2@mail.com", "John Dyadkin");

        UserDto userDto1 = userService.create(newUserRequest1);
        UserDto userDto2 = userService.create(newUserRequest2);

        List<UserDto> users1 = userService.getUsers(null, 0, 10);

        assertEquals(2, users1.size());

        ArrayList<Long> usersIds = new ArrayList<>();
        usersIds.add(userDto2.getId());

        List<UserDto> users2 = userService.getUsers(usersIds, 0, 10);

        assertEquals(1, users2.size());
        assertEquals(userDto2, users2.get(0));
    }

    @Test
    void getUsers_usersNotFound_emptyList() {
        List<UserDto> users = userService.getUsers(null, 0, 10);

        assertTrue(users.isEmpty());
    }

    @Test
    void deleteUser_correct() {
        NewUserRequest newUserRequest1 = new NewUserRequest("mail1@mail.com", "John Tyotkin");
        NewUserRequest newUserRequest2 = new NewUserRequest("mail2@mail.com", "John Dyadkin");

        UserDto userDto1 = userService.create(newUserRequest1);
        UserDto userDto2 = userService.create(newUserRequest2);

        assertEquals(2, userService.getUsers(null, 0, 10).size());

        userService.deleteUser(userDto1.getId());

        assertEquals(1, userService.getUsers(null, 0, 10).size());
    }

    @Test
    void deleteUser_userNotFound_ObjectNotFoundExceptionCust() {
        assertThrows(ObjectNotFoundExceptionCust.class, () -> userService.deleteUser(1L));
    }

    @AfterEach
    public void clearRepository() {
        userRepository.deleteAll();
    }
}