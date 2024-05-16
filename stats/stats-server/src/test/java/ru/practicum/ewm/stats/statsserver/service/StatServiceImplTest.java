package ru.practicum.ewm.stats.statsserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.stats.statsserver.exception.InvalidTimePeriodException;
import ru.practicum.ewm.stats.statsserver.repository.JpaStatRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class StatServiceImplTest {
    StatService statService;

    @Mock
    JpaStatRepository jpaRepository;

    @BeforeEach
    public void setUp() {
        statService = new StatServiceImpl(jpaRepository);
    }

    @Test
    void getStats_startAfterEnd_exception() {
        assertThrows(InvalidTimePeriodException.class,
                () -> statService.getStats(LocalDateTime.now().plusDays(5), LocalDateTime.now().minusDays(5),
                        List.of(), false));
    }

    @Test
    void getStats_startEqualsEnd_exception() {
        LocalDateTime localDateTime = LocalDateTime.now();

        assertThrows(InvalidTimePeriodException.class,
                () -> statService.getStats(localDateTime, localDateTime,
                        List.of(), false));
    }

    @Test
    void getStats_startAfterNow_exception() {
        assertThrows(InvalidTimePeriodException.class,
                () -> statService.getStats(LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(6),
                        List.of(), false));
    }
}