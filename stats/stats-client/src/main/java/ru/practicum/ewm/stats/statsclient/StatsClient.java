package ru.practicum.ewm.stats.statsclient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.ewm.stats.statsdto.EndpointHit;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsClient extends BaseClient {
    @Autowired
    public StatsClient(@Value("${stats-server.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                        .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                        .build()
        );
    }

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void create(EndpointHit endpointHitDto) {
        post("/hit", endpointHitDto);
    }

    public ResponseEntity<Object> getStats(String encodedStart, String encodedEnd,
                                           List<String> uris, Boolean unique) {
        Map<String, Object> parameters = new HashMap<>(Map.of(
                "start", encodedStart,
                "end", encodedEnd,
                "uris", String.join(",", uris),
                "unique", unique
        ));

//        uris.ifPresent(uri -> parameters.put("uris", String.join(",", uri)));

        StringBuilder pathBuilder = new StringBuilder("/stats?start={start}&end={end}&unique={unique}");

        if (parameters.containsKey("uris")) {
            pathBuilder.append(parameters.get("&uris={uris}"));
        }

        return get(pathBuilder.toString(), parameters);
    }
}
