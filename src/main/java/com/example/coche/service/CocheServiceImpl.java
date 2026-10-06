package com.example.coche.service;

import com.example.coche.exception.CocheNoEncontradoException;
import com.example.coche.log.RegistroTransacciones;
import com.example.coche.model.Coche;
import com.example.coche.repository.CocheRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CocheServiceImpl implements CocheService {

    private final CocheRepository cocheRepository;
    private final RegistroTransacciones registro;

    public CocheServiceImpl(CocheRepository cocheRepository, RegistroTransacciones registro) {
        this.cocheRepository = cocheRepository;
        this.registro = registro;
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
    public Coche save(Coche coche, String ip) {
        try {
            Coche guardado = cocheRepository.save(coche);
            registro.registrar(ip, "CREAR", guardado.getId(),
                    coche.getMarca() + " " + coche.getModelo(), "OK");
            return guardado;
        } catch (RuntimeException e) {
            registro.registrar(ip, "CREAR", null, e.getMessage(), "ERROR");
            throw e;
        }
    }

    @Override
    public void update(Long id, Coche coche, String ip) {
        coche.setId(id);
        try {
            findById(id);                       // si no existe, lanza CocheNoEncontradoException
            cocheRepository.update(coche);
            registro.registrar(ip, "EDITAR", id,
                    coche.getMarca() + " " + coche.getModelo(), "OK");
        } catch (RuntimeException e) {
            registro.registrar(ip, "EDITAR", id, e.getMessage(), "ERROR");
            throw e;
        }
    }

    @Override
    public void deleteById(Long id, String ip) {
        try {
            findById(id);                       // si no existe, lanza CocheNoEncontradoException
            cocheRepository.deleteById(id);
            registro.registrar(ip, "ELIMINAR", id, "coche id " + id, "OK");
        } catch (RuntimeException e) {
            registro.registrar(ip, "ELIMINAR", id, e.getMessage(), "ERROR");
            throw e;
        }
    }
}
