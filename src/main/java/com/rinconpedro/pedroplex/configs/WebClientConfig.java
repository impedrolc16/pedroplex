package com.rinconpedro.pedroplex.configs;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${TRAKT_CLIENT_ID}")
    private String traktClientId;

    @Value("${TRAKT_API_URL}")
    private String traktApiUrl;

    @Value("${TMDB_API_URL}")
    private String tmdbApiUrl;

    private HttpClient createHttpClient(String name) {
        return HttpClient.create(
                ConnectionProvider.builder(name)
                        .maxConnections(100)
                        .maxIdleTime(Duration.ofSeconds(30))
                        .build()
        )
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .responseTimeout(Duration.ofSeconds(15))
        .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(15, TimeUnit.SECONDS)));
    }

    @Bean
    public WebClient traktClient() {
        return WebClient.builder()
                .baseUrl(traktApiUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient("trakt")))
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("trakt-api-version", "2")
                .defaultHeader("trakt-api-key", traktClientId)
                .build();
    }

    @Bean
    public WebClient tmdbClient() {
        return WebClient.builder()
                .baseUrl(tmdbApiUrl)
                .clientConnector(new ReactorClientHttpConnector(createHttpClient("tmdb")))
                .build();
    }
}