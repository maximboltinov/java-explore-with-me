package ru.practicum.ewm.mainservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.mainservice.dto.compilation.CompilationDto;
import ru.practicum.ewm.mainservice.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.mainservice.model.Compilation;
import ru.practicum.ewm.mainservice.model.Event;

import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {
    public CompilationDto compilationToCompilationDto(Compilation compilation) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(compilation.getEvents().stream()
                        .map(EventMapper::eventToEventShortDto)
                        .collect(Collectors.toList()))
                .pinned(compilation.isPinned())
                .title(compilation.getTitle())
                .build();
    }

    public Compilation newCompilationDtoToCompilation(NewCompilationDto compilationDto, List<Event> events) {
        return Compilation.builder()
                .events(events)
                .pinned(compilationDto.getPinned())
                .title(compilationDto.getTitle())
                .build();
    }
}
