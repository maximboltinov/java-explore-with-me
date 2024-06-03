
package ru.practicum.ewm.mainservice.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.model.User;
import ru.practicum.ewm.mainservice.repository.JpaUserRepository;
import ru.practicum.ewm.mainservice.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private JpaUserRepository jpaUserRepository;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        userService = new UserServiceImpl(jpaUserRepository);
    }

    @Test
    void getUsers_withNullOrEmptyIdsList_invokeFindAll() {
        when(jpaUserRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty());

        userService.getUsers(List.of(), 0, 10);

        verify(jpaUserRepository, never()).getByIdIn(anyList(), any(Pageable.class));
        verify(jpaUserRepository).findAll(any(Pageable.class));
    }

    @Test
    void getUsers_withNotNullOrEmptyIdsList_invokeGetByIdIn() {
        Pageable page = PageRequest.of(0 / 10, 10);
        List<Long> list = List.of(1L, 2L);
        when(jpaUserRepository.getByIdIn(list, page))
                .thenReturn(List.of());

        userService.getUsers(list, 0, 10);

        verify(jpaUserRepository).getByIdIn(anyList(), any(Pageable.class));
        verify(jpaUserRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void deleteUser_userIsNotFound_ObjectNotFoundExceptionCust() {
        when(jpaUserRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class, () -> userService.deleteUser(anyLong()));
    }

    @Test
    void checkUserById_whenUserAbsent_ObjectNotFoundExceptionCust() {
        when(jpaUserRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class, () -> userService.checkUserById(anyLong()));
    }

    @Test
    void checkUserById_whenUserAvailable_User() {
        User user = User.builder().name("name").email("mail@mail.com").id(1L).build();
        when(jpaUserRepository.findById(anyLong()))
                .thenReturn(Optional.of(user));

        User userResult = userService.checkUserById(anyLong());

        assertEquals(user, userResult);
    }
}