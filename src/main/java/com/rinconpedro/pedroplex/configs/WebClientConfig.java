package com.rinconpedro.pedroplex.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${TRAKT_CLIENT_ID}")
    private String traktClientId;

    @Value("${TRAKT_USERNAME}")
    private String traktUsername;

    @Value("${TMDB_API_KEY}")
    private String tmdbApiKey;

    @Value("${TRAKT_API_URL}")
    private String traktApiUrl;

    @Value("${TMDB_API_URL}")
    private String tmdbApiUrl;

    @Bean
    public WebClient traktClient() {
        return WebClient.builder()
                .baseUrl(traktApiUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("trakt-api-version", "2")
                .defaultHeader("trakt-api-key", traktClientId)
                .build();
    }

    @Bean
    public WebClient tmdbClient() {
        return WebClient.builder()
                .baseUrl(tmdbApiUrl)
                .build();
    }
}