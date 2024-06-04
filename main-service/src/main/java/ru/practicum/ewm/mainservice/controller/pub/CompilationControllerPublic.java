package ru.practicum.ewm.mainservice.controller.pub;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.mainservice.dto.compilation.CompilationDto;
import ru.practicum.ewm.mainservice.service.CompilationService;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@RequestMapping("/compilations")
@AllArgsConstructor
@Slf4j
@Validated
public class CompilationControllerPublic {
    private CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> getCompilations(@RequestParam(required = false) Boolean pinned,
                                                @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Запрос GET /complications?pinned={}&from={}&size={}", pinned, from, size);
        List<CompilationDto> result = compilationService.getCompilations(pinned, from, size);
        log.info("Запрос GET /complications?pinned={}&from={}&size={} {}", pinned, from, size, result);
        return result;
    }

    @GetMapping("/{compId}")
    public CompilationDto getCompilationsById(@PathVariable @Positive Long compId) {
        log.info("Запрос GET /complications/{}", compId);
        CompilationDto result = compilationService.getCompilationsById(compId);
        log.info("Ответ GET /complications/{} {}", compId, result);
        return result;
    }
}
