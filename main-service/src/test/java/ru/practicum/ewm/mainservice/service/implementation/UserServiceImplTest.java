package ru.practicum.ewm.mainservice.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.repository.JpaUserRepository;
import ru.practicum.ewm.mainservice.service.UserService;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    UserService userService;

    @Mock
    JpaUserRepository jpaUserRepository;

    @BeforeEach
    public void setUp() {
        userService = new UserServiceImpl(jpaUserRepository);
    }

    @Test
    void deleteUser() {
        assertThrows(ObjectNotFoundExceptionCust.class, () -> userService.deleteUser(100500L));
    }
}