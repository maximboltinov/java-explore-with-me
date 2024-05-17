package ru.practicum.ewm.stats.statsdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@Builder
public class EndpointHit {
    @NotBlank
    @Size(max = 255)
    private String app;
    @NotBlank
    @Size(max = 255)
    private String uri;
    @NotBlank
    @Size(max = 39)
    private String ip;
    @NotBlank
    @Size(max = 19, min = 19)
    private String timestamp;
}
