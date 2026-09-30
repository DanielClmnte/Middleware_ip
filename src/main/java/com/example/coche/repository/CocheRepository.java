package com.example.coche.repository;

import com.example.coche.model.Coche;

import java.util.List;
import java.util.Optional;

public interface CocheRepository {

    List<Coche> findAll();

    List<Coche> buscar(String marca, String combustible, String transmision, int pagina, int tamanioPagina);

    long contar(String marca, String combustible, String transmision);

    List<String> findDistinctMarcas();

    List<String> findDistinctCombustibles();

    List<String> findDistinctTransmisiones();

    Optional<Coche> findById(Long id);

    Coche save(Coche coche);

    void update(Coche coche);

    void deleteById(Long id);
}
