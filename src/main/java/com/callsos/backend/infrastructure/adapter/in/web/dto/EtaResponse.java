/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.infrastructure.adapter.in.web.dto;

import com.callsos.backend.domain.enums.CategoriaDistancia;

/**
 * DTO de salida para el ETA del denunciante (Épica 4).
 *
 * Deliberadamente sin lat/lon — ver EtaInfo, de donde se mapea 1:1.
 */
public class EtaResponse {

    private final Integer minutosEstimados;
    private final CategoriaDistancia categoriaDistancia;

    public EtaResponse(Integer minutosEstimados, CategoriaDistancia categoriaDistancia) {
        this.minutosEstimados  = minutosEstimados;
        this.categoriaDistancia = categoriaDistancia;
    }

    public Integer getMinutosEstimados()             { return minutosEstimados; }
    public CategoriaDistancia getCategoriaDistancia() { return categoriaDistancia; }
}
