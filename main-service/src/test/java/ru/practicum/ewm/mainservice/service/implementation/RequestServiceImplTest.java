package ru.practicum.ewm.mainservice.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.mainservice.enums.RequestStatus;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.model.ParticipationRequest;
import ru.practicum.ewm.mainservice.repository.JpaEventRepository;
import ru.practicum.ewm.mainservice.repository.JpaRequestRepository;
import ru.practicum.ewm.mainservice.service.RequestService;
import ru.practicum.ewm.mainservice.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {
    private RequestService requestService;

    @Mock
    private JpaRequestRepository jpaRequestRepository;
    @Mock
    private UserService userService;
    @Mock
    private JpaEventRepository jpaEventRepository;

    @BeforeEach
    public void setUp() {
        requestService = new RequestServiceImpl(jpaRequestRepository, userService, jpaEventRepository);
    }

    @Test
    void getRequestsByEventIdAndIdsAndStatus() {
        requestService.getRequestsByEventId(1L);

        verify(jpaRequestRepository).findAllByEventId(1L);
    }

    @Test
    void countByEventIdAndStatus() {
        requestService.countByEventIdAndStatus(1L, RequestStatus.CONFIRMED);

        verify(jpaRequestRepository).countByEventIdAndStatus(1L, RequestStatus.CONFIRMED);
    }

    @Test
    void saveAll() {
        requestService.saveAll(List.of(new ParticipationRequest()));

        verify(jpaRequestRepository).saveAll(List.of(new ParticipationRequest()));
    }

    @Test
    void create_eventNotFound_ObjectNotFoundExceptionCust() {
        when(jpaEventRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class, () -> requestService.create(1L, 1L));
    }
}