package com.example.coche.repository;

import com.example.coche.conexion.ConexionBD;
import com.example.coche.model.Coche;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CocheDAOJDBC implements CocheRepository {

    private static final String SQL_SELECT_ALL =
            "SELECT id, marca, modelo, matricula, anio, color, precio, kilometraje, combustible, transmision " +
                    "FROM coche ORDER BY id";

    private static final String SQL_SELECT_BY_ID =
            "SELECT id, marca, modelo, matricula, anio, color, precio, kilometraje, combustible, transmision " +
                    "FROM coche WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO coche (marca, modelo, matricula, anio, color, precio, kilometraje, combustible, transmision) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE coche SET marca = ?, modelo = ?, matricula = ?, anio = ?, color = ?, precio = ?, " +
                    "kilometraje = ?, combustible = ?, transmision = ? WHERE id = ?";

    private static final String SQL_DELETE = "DELETE FROM coche WHERE id = ?";

    private Connection obtenerConexion() {
        return ConexionBD.getInstancia().getConexion();
    }

    @Override
    public List<Coche> findAll() {
        List<Coche> coches = new ArrayList<>();
        try (PreparedStatement stmt = obtenerConexion().prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                coches.add(mapearCoche(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener la lista de coches", e);
        }
        return coches;
    }

    @Override
    public List<Coche> buscar(String marca, String combustible, String transmision, int pagina, int tamanioPagina) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, marca, modelo, matricula, anio, color, precio, kilometraje, combustible, transmision " +
                        "FROM coche WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        anadirFiltros(sql, parametros, marca, combustible, transmision);
        sql.append(" ORDER BY id LIMIT ? OFFSET ?");
        parametros.add(tamanioPagina);
        parametros.add(pagina * tamanioPagina);

        try (PreparedStatement stmt = obtenerConexion().prepareStatement(sql.toString())) {
            establecerParametrosFiltro(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Coche> coches = new ArrayList<>();
                while (rs.next()) {
                    coches.add(mapearCoche(rs));
                }
                return coches;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar coches", e);
        }
    }

    @Override
    public long contar(String marca, String combustible, String transmision) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM coche WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        anadirFiltros(sql, parametros, marca, combustible, transmision);

        try (PreparedStatement stmt = obtenerConexion().prepareStatement(sql.toString())) {
            establecerParametrosFiltro(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar coches", e);
        }
    }

    @Override
    public List<String> findDistinctMarcas() {
        return findDistinctColumna("marca");
    }

    @Override
    public List<String> findDistinctCombustibles() {
        return findDistinctColumna("combustible");
    }

    @Override
    public List<String> findDistinctTransmisiones() {
        return findDistinctColumna("transmision");
    }

    private List<String> findDistinctColumna(String columna) {
        List<String> valores = new ArrayList<>();
        String sql = "SELECT DISTINCT " + columna + " FROM coche ORDER BY " + columna;
        try (PreparedStatement stmt = obtenerConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                valores.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener los valores distintos de " + columna, e);
        }
        return valores;
    }

    private void anadirFiltros(StringBuilder sql, List<Object> parametros,
                                String marca, String combustible, String transmision) {
        if (marca != null && !marca.isBlank()) {
            sql.append(" AND marca = ?");
            parametros.add(marca);
        }
        if (combustible != null && !combustible.isBlank()) {
            sql.append(" AND combustible = ?");
            parametros.add(combustible);
        }
        if (transmision != null && !transmision.isBlank()) {
            sql.append(" AND transmision = ?");
            parametros.add(transmision);
        }
    }

    private void establecerParametrosFiltro(PreparedStatement stmt, List<Object> parametros) throws SQLException {
        for (int i = 0; i < parametros.size(); i++) {
            stmt.setObject(i + 1, parametros.get(i));
        }
    }

    @Override
    public Optional<Coche> findById(Long id) {
        try (PreparedStatement stmt = obtenerConexion().prepareStatement(SQL_SELECT_BY_ID)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCoche(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar el coche con id " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Coche save(Coche coche) {
        try (PreparedStatement stmt = obtenerConexion().prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            rellenarParametros(stmt, coche);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    coche.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el coche", e);
        }
        return coche;
    }

    @Override
    public void update(Coche coche) {
        try (PreparedStatement stmt = obtenerConexion().prepareStatement(SQL_UPDATE)) {

            rellenarParametros(stmt, coche);
            stmt.setLong(10, coche.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el coche con id " + coche.getId(), e);
        }
    }

    @Override
    public void deleteById(Long id) {
        try (PreparedStatement stmt = obtenerConexion().prepareStatement(SQL_DELETE)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el coche con id " + id, e);
        }
    }

    private void rellenarParametros(PreparedStatement stmt, Coche coche) throws SQLException {
        stmt.setString(1, coche.getMarca());
        stmt.setString(2, coche.getModelo());
        stmt.setString(3, coche.getMatricula());
        stmt.setInt(4, coche.getAnio());
        stmt.setString(5, coche.getColor());
        stmt.setBigDecimal(6, coche.getPrecio());
        stmt.setInt(7, coche.getKilometraje());
        stmt.setString(8, coche.getCombustible());
        stmt.setString(9, coche.getTransmision());
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
