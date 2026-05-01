package com.rinconpedro.pedroplex.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
public class TheMovieDBService {

    private final WebClient tmdbClient;

    @Value("${TMDB_API_KEY}")
    private String tmdbApiKey;

    private final Cache<String, Mono<Map<String, Object>>> cache = Caffeine.newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        .maximumSize(5000)
        .build();

    public TheMovieDBService(WebClient tmdbClient) {
        this.tmdbClient = tmdbClient;
    }

    public Mono<Map<String, Object>> getDetalles(Integer tmdbId, String tipo) {
        String key = tipo + "_" + tmdbId;
        return cache.get(key, k -> tmdbClient.get()
            .uri("/{tipo}/{id}?api_key={apiKey}&language=es-ES", tipo, tmdbId, tmdbApiKey)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests))
            .cache());
    }
}
