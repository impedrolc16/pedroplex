package com.rinconpedro.pedroplex.services;

import com.rinconpedro.pedroplex.models.Multimedia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TraktService {

    private final WebClient traktClient;
    private final TheMovieDBService tmdbService;

    @Value("${TRAKT_USERNAME}")
    private String username;

    // Limita la concurrencia a 10 llamadas paralelas a TMDB
    private static final int PARALLEL_LIMIT = 10;

    public TraktService(WebClient traktClient, TheMovieDBService tmdbService) {
        this.traktClient = traktClient;
        this.tmdbService = tmdbService;
    }

    public Mono<List<Multimedia>> getSeriesCompletadas() {

        Mono<List<Map<String, Object>>> watchedMono = traktClient.get()
                .uri("/users/{username}/watched/shows", username)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList();

        Mono<List<Map<String, Object>>> ratingsMono = traktClient.get()
                .uri("/users/{username}/ratings/shows", username)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList();

        return Mono.zip(watchedMono, ratingsMono)
                .flatMap(tuple -> {

                    List<Map<String, Object>> watched = tuple.getT1();
                    List<Map<String, Object>> ratings = tuple.getT2();

                    // IDs de series vistas
                    Set<Integer> watchedIds = watched.stream()
                            .map(w -> {
                                Map<String, Object> show = (Map<String, Object>) w.get("show");
                                if (show == null) return null;
                                Map<String, Object> ids = (Map<String, Object>) show.get("ids");
                                if (ids == null || ids.get("tmdb") == null) return null;
                                return ((Number) ids.get("tmdb")).intValue();
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    // Ratings SOLO de series vistas
                    List<Map<String, Object>> completadas = ratings.stream()
                            .filter(r -> {
                                Map<String, Object> show = (Map<String, Object>) r.get("show");
                                if (show == null) return false;
                                Map<String, Object> ids = (Map<String, Object>) show.get("ids");
                                if (ids == null || ids.get("tmdb") == null) return false;
                                Integer tmdbId = ((Number) ids.get("tmdb")).intValue();
                                return watchedIds.contains(tmdbId);
                            })
                            .collect(Collectors.toList());

                    return Flux.fromIterable(completadas)
                            .distinct(r -> {
                                Map<String, Object> show = (Map<String, Object>) r.get("show");
                                Map<String, Object> ids = (Map<String, Object>) show.get("ids");
                                return ((Number) ids.get("tmdb")).intValue();
                            })
                            .flatMap(r -> {
                                Map<String, Object> show = (Map<String, Object>) r.get("show");
                                Map<String, Object> ids = (Map<String, Object>) show.get("ids");
                                Integer tmdbId = ((Number) ids.get("tmdb")).intValue();
                                int rating = ((Number) r.get("rating")).intValue();

                                return tmdbService.getDetalles(tmdbId, "tv")
                                        .map(details -> {
                                            Multimedia m = new Multimedia();
                                            m.setTmdbId(tmdbId);
                                            m.setTitulo((String) details.get("name"));
                                            m.setDescripcion((String) details.get("overview"));
                                            String poster = (String) details.get("poster_path");
                                            m.setImagen(poster != null ? "https://image.tmdb.org/t/p/w500" + poster : null);
                                            m.setRating((double) rating);
                                            m.setTipo("serie");
                                            return m;
                                        });
                            }, PARALLEL_LIMIT)
                            .collectList();
                });
    }

    public Mono<List<Multimedia>> getSeriesEnProceso() {

        Mono<List<Map<String, Object>>> watchedMono = traktClient.get()
                .uri("/users/{username}/watched/shows", username)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList();

        Mono<List<Map<String, Object>>> ratingsMono = traktClient.get()
                .uri("/users/{username}/ratings/shows", username)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .collectList();

        return Mono.zip(watchedMono, ratingsMono)
                .flatMap(tuple -> {

                    List<Map<String, Object>> watched = tuple.getT1();
                    List<Map<String, Object>> ratings = tuple.getT2();

                    // IDs de series con rating
                    Set<Integer> ratedIds = ratings.stream()
                            .map(r -> {
                                Map<String, Object> show = (Map<String, Object>) r.get("show");
                                if (show == null) return null;
                                Map<String, Object> ids = (Map<String, Object>) show.get("ids");
                                if (ids == null || ids.get("tmdb") == null) return null;
                                return ((Number) ids.get("tmdb")).intValue();
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    // En proceso = vistas pero sin rating
                    List<Map<String, Object>> enProceso = watched.stream()
                            .filter(w -> {
                                Map<String, Object> show = (Map<String, Object>) w.get("show");
                                if (show == null) return false;
                                Map<String, Object> ids = (Map<String, Object>) show.get("ids");
                                if (ids == null || ids.get("tmdb") == null) return false;
                                Integer tmdbId = ((Number) ids.get("tmdb")).intValue();
                                return !ratedIds.contains(tmdbId);
                            })
                            .collect(Collectors.toList());

                    return Flux.fromIterable(enProceso)
                            .distinct(w -> {
                                Map<String, Object> show = (Map<String, Object>) w.get("show");
                                Map<String, Object> ids = (Map<String, Object>) show.get("ids");
                                return ((Number) ids.get("tmdb")).intValue();
                            })
                            .flatMap(w -> {
                                Map<String, Object> show = (Map<String, Object>) w.get("show");
                                Map<String, Object> ids = (Map<String, Object>) show.get("ids");
                                Integer tmdbId = ((Number) ids.get("tmdb")).intValue();

                                return tmdbService.getDetalles(tmdbId, "tv")
                                        .map(details -> {
                                            Multimedia m = new Multimedia();
                                            m.setTmdbId(tmdbId);
                                            m.setTitulo((String) details.get("name"));
                                            m.setDescripcion((String) details.get("overview"));
                                            String poster = (String) details.get("poster_path");
                                            m.setImagen(poster != null ? "https://image.tmdb.org/t/p/w500" + poster : null);
                                            m.setRating(null);
                                            m.setTipo("serie");
                                            return m;
                                        });
                            }, PARALLEL_LIMIT)
                            .collectList();
                });
    }

    public Mono<List<Multimedia>> getPeliculas() {
        return traktClient.get()
                .uri("/users/{username}/ratings/movies", username)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .filter(map -> {
                    Map<String, Object> movie = (Map<String, Object>) map.get("movie");
                    if (movie == null) return false;
                    Map<String, Object> ids = (Map<String, Object>) movie.get("ids");
                    return ids != null && ids.get("tmdb") != null;
                })
                .distinct(map -> {
                    Map<String, Object> movie = (Map<String, Object>) map.get("movie");
                    Map<String, Object> ids = (Map<String, Object>) movie.get("ids");
                    return ((Number) ids.get("tmdb")).intValue();
                })
                .flatMap(map -> {
                    Map<String, Object> movie = (Map<String, Object>) map.get("movie");
                    Map<String, Object> ids = (Map<String, Object>) movie.get("ids");
                    Integer tmdbId = ((Number) ids.get("tmdb")).intValue();
                    int rating = ((Number) map.get("rating")).intValue();
                    return tmdbService.getDetalles(tmdbId, "movie")
                            .map(details -> {
                                String titulo = (String) details.get("title");
                                String descripcion = (String) details.get("overview");
                                String posterPath = (String) details.get("poster_path");
                                String imagen = posterPath != null ? "https://image.tmdb.org/t/p/w500" + posterPath : null;
                                Multimedia m = new Multimedia();
                                m.setTitulo(titulo);
                                m.setDescripcion(descripcion);
                                m.setImagen(imagen);
                                m.setTmdbId(tmdbId);
                                m.setRating((double) rating);
                                m.setTipo("pelicula");
                                return m;
                            });
                }, PARALLEL_LIMIT)
                .collectList();
    }

    public Mono<List<Multimedia>> getWatchlist() {
        return traktClient.get()
                .uri("/users/{username}/watchlist", username)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .filter(map -> {
                    String type = (String) map.get("type");
                    if ("show".equals(type)) {
                        Map<String, Object> show = (Map<String, Object>) map.get("show");
                        if (show == null) return false;
                        Map<String, Object> ids = (Map<String, Object>) show.get("ids");
                        return ids != null && ids.get("tmdb") != null;
                    } else if ("movie".equals(type)) {
                        Map<String, Object> movie = (Map<String, Object>) map.get("movie");
                        if (movie == null) return false;
                        Map<String, Object> ids = (Map<String, Object>) movie.get("ids");
                        return ids != null && ids.get("tmdb") != null;
                    }
                    return false;
                })
                .distinct(map -> {
                    String type = (String) map.get("type");
                    if ("show".equals(type)) {
                        Map<String, Object> show = (Map<String, Object>) map.get("show");
                        Map<String, Object> ids = (Map<String, Object>) show.get("ids");
                        return ((Number) ids.get("tmdb")).intValue();
                    } else {
                        Map<String, Object> movie = (Map<String, Object>) map.get("movie");
                        Map<String, Object> ids = (Map<String, Object>) movie.get("ids");
                        return ((Number) ids.get("tmdb")).intValue();
                    }
                })
                .flatMap(map -> {
                    String type = (String) map.get("type");
                    Integer tmdbId;
                    String traktTipo;
                    String finalTipo;
                    if ("show".equals(type)) {
                        Map<String, Object> show = (Map<String, Object>) map.get("show");
                        Map<String, Object> ids = (Map<String, Object>) show.get("ids");
                        tmdbId = ((Number) ids.get("tmdb")).intValue();
                        traktTipo = "tv";
                        finalTipo = "serie";
                    } else {
                        Map<String, Object> movie = (Map<String, Object>) map.get("movie");
                        Map<String, Object> ids = (Map<String, Object>) movie.get("ids");
                        tmdbId = ((Number) ids.get("tmdb")).intValue();
                        traktTipo = "movie";
                        finalTipo = "pelicula";
                    }
                    return tmdbService.getDetalles(tmdbId, traktTipo)
                            .map(details -> {
                                String tituloKey = "tv".equals(traktTipo) ? "name" : "title";
                                String titulo = (String) details.get(tituloKey);
                                String descripcion = (String) details.get("overview");
                                String posterPath = (String) details.get("poster_path");
                                String imagen = posterPath != null ? "https://image.tmdb.org/t/p/w500" + posterPath : null;
                                Multimedia m = new Multimedia();
                                m.setTitulo(titulo);
                                m.setDescripcion(descripcion);
                                m.setImagen(imagen);
                                m.setTmdbId(tmdbId);
                                m.setRating(null);
                                m.setTipo(finalTipo);
                                return m;
                            });
                }, PARALLEL_LIMIT)
                .collectList();
    }
}