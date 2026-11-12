package com.example.coche.repository;

import com.example.coche.conexion.ConexionBD;
import com.example.coche.model.Coche;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class CocheDAOJDBC implements CocheRepository {

    private static final String SELECT = "SELECT id, marca, modelo, matricula, anio, color, precio, "
            + "kilometraje, combustible, transmision FROM coche";
    private static final String INSERT = "INSERT INTO coche (marca, modelo, matricula, anio, color, precio, "
            + "kilometraje, combustible, transmision) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE coche SET marca = ?, modelo = ?, matricula = ?, anio = ?, "
            + "color = ?, precio = ?, kilometraje = ?, combustible = ?, transmision = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM coche WHERE id = ?";

    @Override
    public List<Coche> buscar(String marca, String combustible, String transmision, int pagina, int tamanioPagina) {
        List<Object> parametros = new ArrayList<>();
        String sql = SELECT + filtros(parametros, marca, combustible, transmision) + " ORDER BY id LIMIT ? OFFSET ?";
        parametros.add(tamanioPagina);
        parametros.add(pagina * tamanioPagina);

        List<Coche> coches = new ArrayList<>();
        try (PreparedStatement stmt = preparar(sql, parametros); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                coches.add(mapearCoche(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar coches", e);
        }
        return coches;
    }

    @Override
    public long contar(String marca, String combustible, String transmision) {
        List<Object> parametros = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM coche" + filtros(parametros, marca, combustible, transmision);

        try (PreparedStatement stmt = preparar(sql, parametros); ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar coches", e);
        }
    }

    // ¿Hay otro coche (distinto del idExcluido) con esa matricula? Para un coche nuevo se pasa 0.
    @Override
    public boolean existeMatricula(String matricula, long idExcluido) {
        try (PreparedStatement stmt = preparar("SELECT COUNT(*) FROM coche WHERE matricula = ? AND id <> ?",
                List.of(matricula, idExcluido));
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getLong(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al comprobar la matricula " + matricula, e);
        }
    }

    @Override
    public List<String> findDistinctMarcas() {
        return valoresDistintos("marca");
    }

    @Override
    public List<String> findDistinctCombustibles() {
        return valoresDistintos("combustible");
    }

    @Override
    public List<String> findDistinctTransmisiones() {
        return valoresDistintos("transmision");
    }

    @Override
    public Optional<Coche> findById(Long id) {
        try (PreparedStatement stmt = preparar(SELECT + " WHERE id = ?", List.of(id));
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? Optional.of(mapearCoche(rs)) : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el coche con id " + id, e);
        }
    }

    @Override
    public Coche save(Coche coche) {
        try (PreparedStatement stmt = preparar(INSERT, datos(coche))) {
            stmt.executeUpdate();
            try (ResultSet claves = stmt.getGeneratedKeys()) {
                claves.next();
                coche.setId(claves.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el coche", e);
        }
        return coche;
    }

    @Override
    public void update(Coche coche) {
        List<Object> parametros = datos(coche);
        parametros.add(coche.getId());

        try (PreparedStatement stmt = preparar(UPDATE, parametros)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el coche con id " + coche.getId(), e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try (PreparedStatement stmt = preparar(DELETE, List.of(id))) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el coche con id " + id, e);
        }
    }

    // Prepara la sentencia y le pone los parametros (los "?") en orden.
    // Pedimos las claves generadas para poder leer el id nuevo despues de un INSERT.
    private PreparedStatement preparar(String sql, List<Object> parametros) throws SQLException {
        PreparedStatement stmt = ConexionBD.getInstancia().getConexion()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < parametros.size(); i++) {
            stmt.setObject(i + 1, parametros.get(i));
        }
        return stmt;
    }

    // Añade al SQL un WHERE con solo los filtros que el usuario ha elegido
    private String filtros(List<Object> parametros, String marca, String combustible, String transmision) {
        String where = " WHERE 1 = 1";
        if (marca != null && !marca.isBlank()) {
            where += " AND marca = ?";
            parametros.add(marca);
        }
        if (combustible != null && !combustible.isBlank()) {
            where += " AND combustible = ?";
            parametros.add(combustible);
        }
        if (transmision != null && !transmision.isBlank()) {
            where += " AND transmision = ?";
            parametros.add(transmision);
        }
        return where;
    }

    private List<String> valoresDistintos(String columna) {
        List<String> valores = new ArrayList<>();
        try (PreparedStatement stmt = preparar("SELECT DISTINCT " + columna + " FROM coche ORDER BY " + columna, List.of());
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                valores.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener los valores de " + columna, e);
        }
        return valores;
    }

    // Los 9 datos del coche, en el mismo orden que los "?" del INSERT
    private List<Object> datos(Coche c) {
        return new ArrayList<>(Arrays.asList(c.getMarca(), c.getModelo(), c.getMatricula(), c.getAnio(),
                c.getColor(), c.getPrecio(), c.getKilometraje(), c.getCombustible(), c.getTransmision()));
    }

    private Coche mapearCoche(ResultSet rs) throws SQLException {
        Coche coche = new Coche();
        coche.setId(rs.getLong("id"));
        coche.setMarca(rs.getString("marca"));
        coche.setModelo(rs.getString("modelo"));
        coche.setMatricula(rs.getString("matricula"));
        coche.setAnio(rs.getInt("anio"));
        coche.setColor(rs.getString("color"));
        coche.setPrecio(rs.getBigDecimal("precio"));
        coche.setKilometraje(rs.getInt("kilometraje"));
        coche.setCombustible(rs.getString("combustible"));
        coche.setTransmision(rs.getString("transmision"));
        return coche;
    }
}
