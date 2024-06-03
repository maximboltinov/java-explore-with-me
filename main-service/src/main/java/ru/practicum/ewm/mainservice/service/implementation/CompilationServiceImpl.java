package ru.practicum.ewm.mainservice.service.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        Pageable pageRequest = PageRequest.of(from / size, size, Sort.by(Sort.Direction.ASC, "id"));

        List<Specification<Compilation>> specifications = searchFilterToSpecificationList(pinned);
        List<Compilation> compilations = jpaCompilationRepository.findAll(specifications.stream()
                .reduce(Specification::and).orElse(null), pageRequest).getContent();

        return compilations.stream()
                .map(CompilationMapper::compilationToCompilationDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationsById(Long compId) {
        Compilation compilation = getCompilationWithEvents(compId);

        return CompilationMapper.compilationToCompilationDto(compilation);
    }

    private Compilation updateCompilation(Compilation compilation, UpdateCompilationRequest updateCompilationRequest, List<Event> events) {
        return Compilation.builder()
                .id(compilation.getId())
                .events(events)
                .pinned(updateCompilationRequest.getPinned() == null ? compilation.isPinned() : updateCompilationRequest.getPinned())
                .title(updateCompilationRequest.getTitle() == null ? compilation.getTitle() : updateCompilationRequest.getTitle())
                .build();
    }

    private List<Specification<Compilation>> searchFilterToSpecificationList(Boolean pinned) {
        List<Specification<Compilation>> resultSpecification = new ArrayList<>();
        resultSpecification.add(pinned == null ? null : isPinned(pinned));
        return resultSpecification.stream().filter(Objects::nonNull).collect(Collectors.toList());

    }

    private Specification<Compilation> isPinned(Boolean pinned) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("pinned"), pinned);
    }

    private Compilation getCompilationWithEvents(Long compId) {
        return jpaCompilationRepository.findCompilationWithEventById(compId)
                .orElseThrow(() -> new IllegalArgumentException("Подборка событий не найдена"));
    }
}