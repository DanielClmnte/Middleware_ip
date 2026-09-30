package com.example.coche.service;

import java.util.List;

public class ResultadoPagina<T> {

    private final List<T> contenido;
    private final int paginaActual;
    private final int totalPaginas;
    private final long totalElementos;

    public ResultadoPagina(List<T> contenido, int paginaActual, int totalPaginas, long totalElementos) {
        this.contenido = contenido;
        this.paginaActual = paginaActual;
        this.totalPaginas = totalPaginas;
        this.totalElementos = totalElementos;
    }

    public List<T> getContenido() {
        return contenido;
    }

    public int getPaginaActual() {
        return paginaActual;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public long getTotalElementos() {
        return totalElementos;
    }
}
