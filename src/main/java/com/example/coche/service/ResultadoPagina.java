package com.example.coche.service;

import java.util.List;

// Una pagina de resultados: los coches de esa pagina y los datos para pintar la paginacion
public record ResultadoPagina<T>(List<T> contenido, int paginaActual, int totalPaginas, long totalElementos) {
}
