package com.rinconpedro.pedroplex.controllers;

import com.rinconpedro.pedroplex.models.Multimedia;
import com.rinconpedro.pedroplex.services.TraktService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MultimediaController {

    private final TraktService traktService;

    public MultimediaController(TraktService traktService) {
        this.traktService = traktService;
    }

    @GetMapping("/series/completadas")
    public Mono<List<Multimedia>> getSeriesCompletadas() {
        return traktService.getSeriesCompletadas();
    }

    @GetMapping("/series/enproceso")
    public Mono<List<Multimedia>> getSeriesEnProceso() {
        return traktService.getSeriesEnProceso();
    }

    @GetMapping("/peliculas")
    public Mono<List<Multimedia>> getPeliculas() {
        return traktService.getPeliculas();
    }

    @GetMapping("/watchlist")
    public Mono<List<Multimedia>> getWatchlist() {
        return traktService.getWatchlist();
    }
}
