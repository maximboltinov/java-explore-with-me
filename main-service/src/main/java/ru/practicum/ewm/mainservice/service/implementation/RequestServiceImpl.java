package ru.practicum.ewm.mainservice.service.implementation;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.mainservice.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.mainservice.enums.EventState;
import ru.practicum.ewm.mainservice.enums.RequestStatus;
import ru.practicum.ewm.mainservice.exception.custom.ConflictException;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.mapper.RequestMapper;
import ru.practicum.ewm.mainservice.model.Event;
import ru.practicum.ewm.mainservice.model.ParticipationRequest;
import ru.practicum.ewm.mainservice.model.User;
import ru.practicum.ewm.mainservice.repository.JpaEventRepository;
import ru.practicum.ewm.mainservice.repository.JpaRequestRepository;
import ru.practicum.ewm.mainservice.service.RequestService;
import ru.practicum.ewm.mainservice.service.UserService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.ewm.mainservice.enums.RequestStatus.*;

@Service
@AllArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final JpaRequestRepository jpaRequestRepository;
    private final UserService userService;
    private final JpaEventRepository jpaEventRepository;

    @Override
    public List<ParticipationRequest> getRequestsByEventId(Long eventId) {
        return jpaRequestRepository.findAllByEventId(eventId);
    }

    @Override
    public List<ParticipationRequest> getRequestsByEventIdAndIdsAndStatus(
            Long eventId, Set<Long> requestIds, RequestStatus status) {
        return jpaRequestRepository.findAllByEventIdAndIdInOrderById(eventId, requestIds);
    }

    @Override
    public int countByEventIdAndStatus(Long eventId, RequestStatus requestStatus) {
        return jpaRequestRepository.countByEventIdAndStatus(eventId, requestStatus);
    }

    @Override
    public void saveAll(List<ParticipationRequest> requests) {
        jpaRequestRepository.saveAll(requests);
    }

    @Override
    public ParticipationRequestDto create(Long userId, Long eventId) {
        User user = userService.checkUserById(userId);

        Event event = jpaEventRepository.findById(eventId).orElseThrow(() ->
                new ObjectNotFoundExceptionCust("Не найдено событие с id " + eventId));

        checkUserCanMakeRequest(userId, eventId, event);
        checkParticipationRequestExists(userId, eventId);
        checkEventIsNotPublished(event);

        ParticipationRequest request = createRequest(user, event);
        ParticipationRequest savedRequest = jpaRequestRepository.save(request);
        return RequestMapper.participationRequestToDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId) {
        User user = userService.checkUserById(userId);

        List<ParticipationRequest> requests = jpaRequestRepository.findAllByRequesterId(user.getId());

        return requests.stream()
                .map(RequestMapper::participationRequestToDto).collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        userService.checkUserById(userId);

        ParticipationRequest request = jpaRequestRepository.findById(requestId).orElseThrow(() ->
                new ObjectNotFoundExceptionCust("Запрос с id " + requestId + " не найден"));

        checkUserCanCancelRequest(userId, request);
        request.setStatus(CANCELED);
        jpaRequestRepository.save(request);

        return RequestMapper.participationRequestToDto(request);
    }

    @Override
    public Map<Long, Integer> confirmedRequests(List<Long> eventIds, RequestStatus status) {
        return jpaRequestRepository.countByEventsIdAndStatus(eventIds, status);
    }

    private void checkUserCanMakeRequest(Long userId, Long eventId, Event event) {
        if (Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Пользователь с id = " + userId + " не может делать запрос " +
                    " на свое мероприятие id " + eventId);
        }
    }

    private void checkParticipationRequestExists(Long userId, Long eventId) {
        Optional<ParticipationRequest> participationRequest = jpaRequestRepository.findByRequesterIdAndEventId(userId, eventId);
        if (participationRequest.isPresent()) {
            throw new ConflictException("Заявка на участие от пользователя id " + userId + " на событие id "
                    + eventId + " уже существует");
        }
    }

    private void checkEventIsNotPublished(Event event) {
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Невозможно сделать заявку на неопубликованное событие с id " + event.getId());
        }
    }

    private ParticipationRequest createRequest(User user, Event event) {
        ParticipationRequest request = ParticipationRequest.builder()
                .requester(user)
                .event(event)
                .created(LocalDateTime.now())
                .build();

        System.out.println("user = " + user + ", event = " + event);

        if (event.getConfirmedRequests() == event.getParticipantLimit() && event.getParticipantLimit() != 0) {
            throw new ConflictException("Превышен лимит участников для события с id " + event.getId());
        } else if (event.getParticipantLimit() == 0 || !event.isRequestModeration()) {
            request.setStatus(CONFIRMED);
            addConfirmedRequestToEvent(event);
        } else {
            request.setStatus(PENDING);
        }
        return request;
    }

    private void addConfirmedRequestToEvent(Event event) {
        event.setConfirmedRequests(event.getConfirmedRequests() + 1);
        jpaEventRepository.save(event);
    }

    private void checkUserCanCancelRequest(Long userId, ParticipationRequest request) {
        if (!Objects.equals(request.getRequester().getId(), userId)) {
            throw new ConflictException("Пользователь id '" + userId + " не может отменить заявку id " + request.getId());
        }
    }
}
