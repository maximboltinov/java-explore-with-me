package ru.practicum.ewm.mainservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.ewm.stats.statsdto.EndpointHit;
import ru.practicum.ewm.stats.statsdto.ViewStats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StatsClient extends BaseClient {
    @Autowired
    public StatsClient(@Value("http://localhost:9090") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                        .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                        .build()
        );
    }

    public void create(EndpointHit endpointHitDto) {
        post("/hit", endpointHitDto);
    }

    public ResponseEntity<List<ViewStats>> getStats(String start, String end,
                                                    List<String> uris, Boolean unique) {
        Map<String, Object> parameters = new HashMap<>(Map.of(
                "start", start,
                "end", end,
                "uris", String.join(",", uris),
                "unique", unique
        ));

        return get("/stats?start={start}&end={end}&uris={uris}&unique={unique}", parameters);
    }
}
