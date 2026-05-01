package com.rinconpedro.pedroplex.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Multimedia {

    private String titulo;
    private String descripcion;
    private Integer tmdbId;
    private Double rating;
    private String tipo;
    private String imagen;
}