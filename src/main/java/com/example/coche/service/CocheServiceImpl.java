package com.example.coche.service;

import com.example.coche.exception.CocheNoEncontradoException;
import com.example.coche.exception.DatosInvalidosException;
import com.example.coche.model.Coche;
import com.example.coche.repository.CocheRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CocheServiceImpl implements CocheService {

    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");   // lo que cabe en DECIMAL(10,2)

    private final CocheRepository cocheRepository;
    private final RegistroTransacciones registro;

    public CocheServiceImpl(CocheRepository cocheRepository, RegistroTransacciones registro) {
        this.cocheRepository = cocheRepository;
        this.registro = registro;
    }

    @Override
    public ResultadoPagina<Coche> buscar(String marca, String combustible, String transmision,
                                          int pagina, int tamanioPagina) {
        long total = cocheRepository.contar(marca, combustible, transmision);
        int totalPaginas = (int) Math.ceil((double) total / tamanioPagina);   // 10 coches / 6 = 2 paginas
        int paginaValida = Math.max(0, Math.min(pagina, totalPaginas - 1));   // evita paginas que no existen
        List<Coche> coches = cocheRepository.buscar(marca, combustible, transmision, paginaValida, tamanioPagina);
        return new ResultadoPagina<>(coches, paginaValida, totalPaginas, total);
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
            validar(coche);                     // si los datos no son validos, lanza DatosInvalidosException
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
            validar(coche);
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

    // Comprueba los datos del coche antes de tocar la base de datos
    private void validar(Coche c) {
        if (vacio(c.getMarca()) || vacio(c.getModelo()) || vacio(c.getMatricula()) || vacio(c.getColor())
                || vacio(c.getCombustible()) || vacio(c.getTransmision())
                || c.getAnio() == null || c.getPrecio() == null || c.getKilometraje() == null) {
            throw new DatosInvalidosException("Todos los campos son obligatorios");
        }
        if (c.getMarca().length() > 50 || c.getModelo().length() > 50 || c.getMatricula().length() > 10
                || c.getColor().length() > 30 || c.getCombustible().length() > 20 || c.getTransmision().length() > 20) {
            throw new DatosInvalidosException("Algún texto es demasiado largo");
        }
        if (c.getAnio() < 1900 || c.getAnio() > 2100) {
            throw new DatosInvalidosException("El año debe estar entre 1900 y 2100");
        }
        if (c.getPrecio().signum() < 0 || c.getKilometraje() < 0) {
            throw new DatosInvalidosException("El precio y el kilometraje no pueden ser negativos");
        }
        if (c.getPrecio().compareTo(PRECIO_MAXIMO) > 0) {
            throw new DatosInvalidosException("El precio es demasiado grande");
        }
        long idExcluido = c.getId() == null ? 0 : c.getId();   // al editar, el propio coche no cuenta como repetido
        if (cocheRepository.existeMatricula(c.getMatricula(), idExcluido)) {
            throw new DatosInvalidosException("Ya existe un coche con la matrícula " + c.getMatricula());
        }
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
