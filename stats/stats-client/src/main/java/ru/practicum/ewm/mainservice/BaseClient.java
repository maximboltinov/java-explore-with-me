package ru.practicum.ewm.mainservice;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;
import ru.practicum.ewm.stats.statsdto.ViewStats;

import java.util.List;
import java.util.Map;

public class BaseClient {
    protected final RestTemplate rest;

    public BaseClient(RestTemplate rest) {
        this.rest = rest;
    }

    protected <T> ResponseEntity<List<ViewStats>> post(String path, @Nullable Map<String, Object> parameters, T body) {
        return makeAndSendRequest(HttpMethod.POST, path, parameters, body);
    }

    protected <T> ResponseEntity<List<ViewStats>> post(String path, T body) {
        return post(path, null, body);
    }

    protected ResponseEntity<List<ViewStats>> get(String path, @Nullable Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.GET, path, parameters, null);
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private static ResponseEntity<List<ViewStats>> prepareGatewayResponse(ResponseEntity<List<ViewStats>> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return response;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.getStatusCode());

        if (response.hasBody()) {
            return responseBuilder.body(response.getBody());
        }

        return responseBuilder.build();
    }

    private <T> ResponseEntity<List<ViewStats>> makeAndSendRequest(HttpMethod method, String path,
                                                          @Nullable Map<String, Object> parameters,
                                                          @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders());

        ResponseEntity<List<ViewStats>> statsServerResponse;
        ParameterizedTypeReference<List<ViewStats>> responseType = new ParameterizedTypeReference<>() {
        };

            if (parameters != null) {
                statsServerResponse = rest.exchange(path, method, requestEntity, responseType, parameters);
            } else {
                statsServerResponse = rest.exchange(path, method, requestEntity, responseType);
            }

        return prepareGatewayResponse(statsServerResponse);
    }
}