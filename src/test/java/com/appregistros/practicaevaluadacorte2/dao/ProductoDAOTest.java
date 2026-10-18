package com.appregistros.practicaevaluadacorte2.dao;

import com.appregistros.practicaevaluadacorte2.Database.DatabaseConnection;
import com.appregistros.practicaevaluadacorte2.model.Producto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Pruebas de integración contra la base papeleria_campus configurada en DatabaseConnection.
// Si PostgreSQL no está disponible las pruebas se omiten (no fallan).
// Solo crean productos con código ZTEST*, y los eliminan al terminar cada prueba.
class ProductoDAOTest {

    private static final String PREFIJO = "ZTEST";

    private final ProductoDAO dao = new ProductoDAO();

    @BeforeAll
    static void requiereBaseDeDatos() {
        boolean disponible;
        try (Connection ignored = DatabaseConnection.getConnection()) {
            disponible = true;
        } catch (SQLException ex) {
            disponible = false;
        }
        assumeTrue(disponible, "PostgreSQL no disponible: se omiten las pruebas de integración del DAO");
    }

    @BeforeEach
    void limpiarAntes() throws SQLException {
        borrarProductosDePrueba();
    }

    @AfterEach
    void limpiarDespues() throws SQLException {
        borrarProductosDePrueba();
    }

    private static void borrarProductosDePrueba() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM catalogo.producto WHERE codigo LIKE ?")) {
            statement.setString(1, PREFIJO + "%");
            statement.executeUpdate();
        }
    }

    @Test
    @DisplayName("Ciclo CRUD: crear, buscar, actualizar solo el producto indicado y eliminarlo")
    void cicloCrud() throws Exception {
        assertTrue(dao.guardar(producto(PREFIJO + "1", "Producto de prueba", "15.00", 3)));

        Producto creado = buscarPorCodigo(PREFIJO + "1");
        assertNotNull(creado, "El producto insertado debe poder consultarse");
        assertEquals("Producto de prueba", creado.getNombre());

        List<String> otrosAntes = filasExcepto(creado.getIdProducto());

        creado.setNombre("Producto de prueba editado");
        creado.setPrecio(new BigDecimal("20.50"));
        assertTrue(dao.actualizar(creado));

        Producto actualizado = buscarPorCodigo(PREFIJO + "1");
        assertEquals("Producto de prueba editado", actualizado.getNombre());
        assertEquals(new BigDecimal("20.50"), actualizado.getPrecio());
        assertEquals(otrosAntes, filasExcepto(creado.getIdProducto()), "El UPDATE no debe modificar otras filas");

        assertTrue(dao.eliminarPorId(creado.getIdProducto()));
        assertNull(buscarPorCodigo(PREFIJO + "1"));
        assertEquals(otrosAntes, filasExcepto(creado.getIdProducto()), "El DELETE no debe afectar otras filas");
    }

    @Test
    @DisplayName("E2: código duplicado (23505) devuelve un mensaje seguro y no inserta")
    void codigoDuplicado() throws Exception {
        assertTrue(dao.guardar(producto(PREFIJO + "2", "Original", "10.00", 1)));

        DataAccessException ex = assertThrows(DataAccessException.class,
                () -> dao.guardar(producto(PREFIJO + "2", "Duplicado", "10.00", 1)));

        assertEquals("23505", ex.getSqlState());
        assertEquals("Ya existe otro producto con ese código.", ex.getMessage());
        assertEquals(1, contarPorCodigo(PREFIJO + "2"));
        assertEquals("Original", buscarPorCodigo(PREFIJO + "2").getNombre());
    }

    @Test
    @DisplayName("E2: violación de CHECK (23514) al insertar y al actualizar no cambia la base")
    void violacionCheck() throws Exception {
        DataAccessException alInsertar = assertThrows(DataAccessException.class,
                () -> dao.guardar(producto(PREFIJO + "3", "Precio cero", "0", 1)));
        assertEquals("23514", alInsertar.getSqlState());
        assertEquals("El precio debe ser mayor que cero.", alInsertar.getMessage());
        assertEquals(0, contarPorCodigo(PREFIJO + "3"));

        assertTrue(dao.guardar(producto(PREFIJO + "4", "Stock valido", "10.00", 5)));
        Producto existente = buscarPorCodigo(PREFIJO + "4");
        existente.setStock(-1);

        DataAccessException alActualizar = assertThrows(DataAccessException.class, () -> dao.actualizar(existente));
        assertEquals("23514", alActualizar.getSqlState());
        assertEquals("El stock debe ser mayor que cero.", alActualizar.getMessage());
        assertEquals(5, buscarPorCodigo(PREFIJO + "4").getStock(), "El registro debe conservar su valor anterior");
    }

    @Test
    @DisplayName("E3: UPDATE y DELETE sobre un ID inexistente afectan 0 filas y devuelven false")
    void filasAfectadasCero() throws Exception {
        assertTrue(dao.guardar(producto(PREFIJO + "5", "Temporal", "10.00", 1)));
        Producto temporal = buscarPorCodigo(PREFIJO + "5");
        assertTrue(dao.eliminarPorId(temporal.getIdProducto()));

        assertFalse(dao.actualizar(temporal), "No debe anunciar éxito si el UPDATE afecta 0 filas");
        assertFalse(dao.eliminarPorId(temporal.getIdProducto()), "No debe anunciar éxito si el DELETE afecta 0 filas");
    }

    @Test
    @DisplayName("E4: el mensaje mostrado no expone SQL ni nombres internos")
    void mensajeSeguro() throws Exception {
        assertTrue(dao.guardar(producto(PREFIJO + "6", "Original", "10.00", 1)));

        DataAccessException ex = assertThrows(DataAccessException.class,
                () -> dao.guardar(producto(PREFIJO + "6", "Duplicado", "10.00", 1)));

        String mensaje = ex.getMessage().toLowerCase();
        assertFalse(mensaje.contains("duplicate"));
        assertFalse(mensaje.contains("producto_codigo_key"));
        assertFalse(mensaje.contains("catalogo."));
        assertTrue(ex.getOperacion().startsWith("INSERT producto"));
        assertInstanceOf(SQLException.class, ex.getCause(), "La causa original se conserva para el registro");
    }

    private static Producto producto(String codigo, String nombre, String precio, int stock) {
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setCategoria("Papeleria");
        producto.setPrecio(new BigDecimal(precio));
        producto.setStock(stock);
        return producto;
    }

    private Producto buscarPorCodigo(String codigo) throws DataAccessException {
        return dao.buscar(codigo, null, null).stream()
                .filter(p -> p.getCodigo().equals(codigo))
                .findFirst()
                .orElse(null);
    }

    private int contarPorCodigo(String codigo) throws DataAccessException {
        return (int) dao.buscar(codigo, null, null).stream().filter(p -> p.getCodigo().equals(codigo)).count();
    }

    // Fotografía de todas las filas salvo la indicada, para comprobar que no cambian.
    private static List<String> filasExcepto(int idProducto) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id_producto, codigo, nombre, categoria, precio, stock FROM catalogo.producto "
                             + "WHERE id_producto <> ? ORDER BY id_producto")) {
            statement.setInt(1, idProducto);
            try (ResultSet rs = statement.executeQuery()) {
                StringBuilder filas = new StringBuilder();
                while (rs.next()) {
                    filas.append(rs.getInt(1)).append('|').append(rs.getString(2)).append('|')
                            .append(rs.getString(3)).append('|').append(rs.getString(4)).append('|')
                            .append(rs.getBigDecimal(5)).append('|').append(rs.getInt(6)).append('\n');
                }
                return filas.toString().lines().collect(Collectors.toList());
            }
        }
    }
}
