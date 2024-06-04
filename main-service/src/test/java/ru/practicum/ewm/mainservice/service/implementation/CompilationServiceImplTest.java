package ru.practicum.ewm.mainservice.service.implementation;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.mainservice.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.mainservice.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.model.Compilation;
import ru.practicum.ewm.mainservice.model.Event;
import ru.practicum.ewm.mainservice.repository.JpaCompilationRepository;
import ru.practicum.ewm.mainservice.repository.JpaEventRepository;
import ru.practicum.ewm.mainservice.service.CompilationService;
import ru.practicum.ewm.mainservice.service.EventService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {
    private CompilationService compilationService;

    @Mock
    private JpaEventRepository jpaEventRepository;
    @Mock
    private JpaCompilationRepository jpaCompilationRepository;
    @Mock
    private EventService eventService;

    @Captor
    private ArgumentCaptor<Compilation> compilationArgumentCaptor;

    private final EasyRandom generator = new EasyRandom();

    @BeforeEach
    public void setUp() {
        compilationService = new CompilationServiceImpl(jpaEventRepository, jpaCompilationRepository, eventService);
    }

    @Test
    void createCompilation_correct() {
        NewCompilationDto newCompilationDto =
                NewCompilationDto.builder().events(List.of(1L)).title("compilation").pinned(false).build();
        Event event = Event.builder().id(1L).build();

        when(jpaEventRepository.findAllById(anyList()))
                .thenReturn(List.of(event));

        when(jpaCompilationRepository.save(any(Compilation.class)))
                .thenReturn(Compilation.builder().events(List.of()).build());

        compilationService.createCompilation(newCompilationDto);

        verify(jpaCompilationRepository).save(compilationArgumentCaptor.capture());
        Compilation compilationSaved = compilationArgumentCaptor.getValue();

        assertEquals("compilation", compilationSaved.getTitle());
        assertFalse(compilationSaved.isPinned());
        assertEquals(event, compilationSaved.getEvents().get(0));
    }

    @Test
    void deleteCompilation_notFoundCompilation_ObjectNotFoundExceptionCust() {
        when(jpaCompilationRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class, () -> compilationService.deleteCompilation(1L));

        verify(jpaCompilationRepository, never()).delete(any());
    }

    @Test
    void updateCompilation_notFoundCompilation_ObjectNotFoundExceptionCust() {
        when(jpaCompilationRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundExceptionCust.class,
                () -> compilationService.updateCompilation(1L, new UpdateCompilationRequest()));

        verify(jpaCompilationRepository, never()).save(any());
    }

    @Test
    void updateCompilation_correct() {
        Compilation compilation = generator.nextObject(Compilation.class);
        compilation.setPinned(false);

        UpdateCompilationRequest updateCompilationRequest =
                UpdateCompilationRequest.builder().pinned(true).title("new title").events(List.of(1L)).build();

        Event event = generator.nextObject(Event.class);

        when(jpaCompilationRepository.findById(anyLong()))
                .thenReturn(Optional.of(compilation));

        when(jpaEventRepository.findAllById(anyList()))
                .thenReturn(List.of(event));

        when(jpaCompilationRepository.save(any(Compilation.class)))
                .thenReturn(Compilation.builder().events(List.of()).build());

        compilationService.updateCompilation(1L, updateCompilationRequest);

        verify(jpaCompilationRepository).save(compilationArgumentCaptor.capture());
        Compilation compilationSaved = compilationArgumentCaptor.getValue();

        assertEquals("new title", compilationSaved.getTitle());
        assertTrue(compilationSaved.isPinned());
        assertEquals(event, compilationSaved.getEvents().get(0));
    }
}