package ru.practicum.ewm.mainservice.specification;

import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.mainservice.enums.EventState;
import ru.practicum.ewm.mainservice.model.Event;

import java.time.LocalDateTime;
import java.util.List;

@UtilityClass
public class EventSpecification {
    public static Specification<Event> likeText(String text) {
        if (text == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("annotation")),
                                "%" + text.toLowerCase() + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")),
                                "%" + text.toLowerCase() + "%")
                );
    }

    public static Specification<Event> categoryIn(List<Long> categoryIds) {
        if (categoryIds == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get("category").get("id")).value(categoryIds);
    }

    public static Specification<Event> isPaid(Boolean isPaid) {
        if (isPaid == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("paid"), isPaid);
    }

    public static Specification<Event> eventDateInRange(LocalDateTime startRange, LocalDateTime endRange) {
        if (startRange == null || endRange == null) {
            return eventDateAfter(LocalDateTime.now());
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("eventDate"), startRange, endRange);
    }

    public static Specification<Event> eventDateBeforeOrEqual(LocalDateTime endRange) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), endRange);
    }

    public static Specification<Event> eventDateAfterOrEqual(LocalDateTime startRange) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), startRange);
    }

    public static Specification<Event> eventDateAfter(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get("eventDate"), dateTime);
    }

    public static Specification<Event> eventStatusEquals(EventState eventState) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("state"), eventState);
    }

    public static Specification<Event> eventStatusIn(List<EventState> states) {
        if (states == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get("state")).value(states);
    }

    public static Specification<Event> initiatorIdIn(List<Long> userIds) {
        if (userIds == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.in(root.get("initiator").get("id")).value(userIds);
    }

    public static Specification<Event> isAvailable(boolean isAvailable) {
        if (!isAvailable) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get("participantLimit"),
                root.get("numberOfParticipants"));
    }
}