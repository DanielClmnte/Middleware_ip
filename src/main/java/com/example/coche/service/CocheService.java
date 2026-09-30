package com.example.coche.service;

import com.example.coche.model.Coche;

import java.util.List;

public interface CocheService {

    List<Coche> findAll();

    ResultadoPagina<Coche> buscar(String marca, String combustible, String transmision, int pagina, int tamanioPagina);

    List<String> listarMarcasDisponibles();

    List<String> listarCombustiblesDisponibles();

    List<String> listarTransmisionesDisponibles();

    Coche findById(Long id);

    Coche save(Coche coche);

    void update(Long id, Coche coche);

    void deleteById(Long id);
}
