package com.example.coche.service;

import com.example.coche.exception.CocheNoEncontradoException;
import com.example.coche.model.Coche;
import com.example.coche.repository.CocheRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CocheServiceImpl implements CocheService {

    private final CocheRepository cocheRepository;

    public CocheServiceImpl(CocheRepository cocheRepository) {
        this.cocheRepository = cocheRepository;
    }

    @Override
    public List<Coche> findAll() {
        return cocheRepository.findAll();
    }

    @Override
    public ResultadoPagina<Coche> buscar(String marca, String combustible, String transmision,
                                          int pagina, int tamanioPagina) {
        long totalElementos = cocheRepository.contar(marca, combustible, transmision);
        int totalPaginas = (int) Math.max(1, Math.ceil((double) totalElementos / tamanioPagina));
        int paginaSegura = Math.max(0, Math.min(pagina, totalPaginas - 1));
        List<Coche> contenido = cocheRepository.buscar(marca, combustible, transmision, paginaSegura, tamanioPagina);
        return new ResultadoPagina<>(contenido, paginaSegura, totalPaginas, totalElementos);
    }

    @Override
    public List<String> listarMarcasDisponibles() {
        return cocheRepository.findDistinctMarcas();
    }

    @Override
    public List<String> listarCombustiblesDisponibles() {
        return cocheRepository.findDistinctCombustibles();
    }

    @Override
    public List<String> listarTransmisionesDisponibles() {
        return cocheRepository.findDistinctTransmisiones();
    }

    @Override
    public Coche findById(Long id) {
        return cocheRepository.findById(id)
                .orElseThrow(() -> new CocheNoEncontradoException(id));
    }

    @Override
    public Coche save(Coche coche) {
        return cocheRepository.save(coche);
    }

    @Override
    public void update(Long id, Coche coche) {
        findById(id);
        coche.setId(id);
        cocheRepository.update(coche);
    }

    @Override
    public void deleteById(Long id) {
        findById(id);
        cocheRepository.deleteById(id);
    }
}
