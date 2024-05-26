package ru.practicum.ewm.mainservice.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.mainservice.enums.SortType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventRequestParams {
    private String text;
    private List<Long> categories;
    private Boolean paid;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeStart;

    @JsonFormat (pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeEnd;

    private Boolean onlyAvailable = false;

    private SortType sort;

    @Override
    public String toString() {
        return "EventRequestParams{" +
                "text='" + text + '\'' +
                ", categories=" + categories +
                ", paid=" + paid +
                ", rangeStart=" + rangeStart +
                ", rangeEnd=" + rangeEnd +
                ", onlyAvailable=" + onlyAvailable +
                ", sort='" + sort + '\'' +
                '}';
    }
}