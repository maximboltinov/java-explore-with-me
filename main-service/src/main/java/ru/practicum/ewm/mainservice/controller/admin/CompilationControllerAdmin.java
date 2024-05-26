package ru.practicum.ewm.mainservice.controller.admin;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.mainservice.dto.compilation.CompilationDto;
import ru.practicum.ewm.mainservice.dto.compilation.NewCompilationDto;
import ru.practicum.ewm.mainservice.dto.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.mainservice.service.CompilationService;

import javax.validation.Valid;

@RestController
@RequestMapping("/admin/compilations")
@AllArgsConstructor
@Slf4j
@Validated
public class CompilationControllerAdmin {
private final CompilationService compilationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto createCompilation(@RequestBody @Valid NewCompilationDto newCompilationDto) {
        log.info("Запрос POST /admin/compilations {}", newCompilationDto);
        CompilationDto result = compilationService.createCompilation(newCompilationDto);
        log.info("Ответ POST /admin/compilations {}", result);
        return result;
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable Long compId) {
        log.info("Запрос DELETE /admin/compilations/{}", compId);
        compilationService.deleteCompilation(compId);
        log.info("Ответ DELETE /admin/compilations/{} {}", compId, HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{compId}")
    public CompilationDto updateCompilation(@PathVariable long compId,
                                            @RequestBody @Valid UpdateCompilationRequest updateCompilationRequest) {
        log.info("Запрос PATCH /admin/compilations/{} {}", compId, updateCompilationRequest);
        CompilationDto result = compilationService.updateCompilation(compId, updateCompilationRequest);
        log.info("Ответ PATCH /admin/compilations/{} {}", compId, result);
        return result;
    }
}
