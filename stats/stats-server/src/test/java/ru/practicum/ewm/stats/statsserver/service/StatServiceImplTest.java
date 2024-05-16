package ru.practicum.ewm.stats.statsserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.stats.statsserver.repository.JpaStatRepository;

import java.util.Optional;

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
    void getStats() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> statService.getStats("2023-10-11 15-14-13", "jjjjjjjjjj",
                        Optional.empty(), false));
    }
}