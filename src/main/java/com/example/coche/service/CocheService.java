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

    // Las tres operaciones de escritura reciben la IP del cliente para apuntarla en log_transaccion

    Coche save(Coche coche, String ip);

    void update(Long id, Coche coche, String ip);

    void deleteById(Long id, String ip);
}
