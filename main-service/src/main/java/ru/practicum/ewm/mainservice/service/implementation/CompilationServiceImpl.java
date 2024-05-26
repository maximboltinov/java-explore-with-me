package ru.practicum.ewm.mainservice.service.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.mainservice.dto.compilation.CompilationDto;
import ru.practicum.ewm.mainservice.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.mainservice.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.mapper.CompilationMapper;
import ru.practicum.ewm.mainservice.model.Compilation;
import ru.practicum.ewm.mainservice.model.Event;
import ru.practicum.ewm.mainservice.repository.JpaCompilationRepository;
import ru.practicum.ewm.mainservice.repository.JpaEventRepository;
import ru.practicum.ewm.mainservice.service.CompilationService;
import ru.practicum.ewm.mainservice.service.EventService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final JpaEventRepository jpaEventRepository;
    private final JpaCompilationRepository jpaCompilationRepository;
    private final EventService eventService;

    @Override
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        List<Event> events = newCompilationDto.getEvents() != null ?
                jpaEventRepository.findAllById(newCompilationDto.getEvents()) : List.of();

        Compilation compilation = CompilationMapper.newCompilationDtoToCompilation(newCompilationDto, events);
        Compilation savedCompilation = jpaCompilationRepository.save(compilation);

        return CompilationMapper.compilationToCompilationDto(savedCompilation);
    }

    @Override
    public void deleteCompilation(Long compId) {
        Compilation compilation = jpaCompilationRepository.findById(compId).orElseThrow(() ->
                new ObjectNotFoundExceptionCust("Не найдена подборка событий с id " + compId));

        jpaCompilationRepository.delete(compilation);
    }

    @Override
    public CompilationDto updateCompilation(long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = jpaCompilationRepository.findById(compId).orElseThrow(() ->
                new ObjectNotFoundExceptionCust("Не найдена подборка событий с id " + compId));

        List<Event> events = updateCompilationRequest.getEvents() != null ?
                jpaEventRepository.findAllById(updateCompilationRequest.getEvents()) : List.of();

        Compilation savedCompilation = jpaCompilationRepository.save(updateCompilation(compilation,
                updateCompilationRequest, events));

        CompilationDto compilationDto = CompilationMapper.compilationToCompilationDto(savedCompilation);
        eventService.loadShortEventsViewsNumber(compilationDto.getEvents());
        return compilationDto;

    }

    private Compilation updateCompilation(Compilation compilation, UpdateCompilationRequest updateCompilationRequest, List<Event> events) {
        return Compilation.builder()
                .id(compilation.getId())
                .events(events)
                .pinned(updateCompilationRequest.getPinned() == null ? compilation.isPinned() : updateCompilationRequest.getPinned())
                .title(updateCompilationRequest.getTitle() == null ? compilation.getTitle() : updateCompilationRequest.getTitle())
                .build();
    }
}