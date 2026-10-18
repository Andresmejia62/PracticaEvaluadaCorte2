package com.appregistros.practicaevaluadacorte2.dao;

import com.appregistros.practicaevaluadacorte2.Database.DatabaseConnection;
import com.appregistros.practicaevaluadacorte2.model.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    // Se encarga de todas las operaciones de lectura y escritura del catálogo de productos.
    // La capa DAO abstrae la lógica de acceso a datos para que el controlador solo maneje la vista.

    public List<Producto> listarActivos() throws SQLException {
        String sql = "SELECT id_producto, codigo, nombre, categoria, precio, stock FROM catalogo.producto WHERE stock > 0 ORDER BY id_producto";
        List<Producto> productos = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                productos.add(mapearProducto(resultSet));
            }
        }

        return productos;
    }

    // Devuelve las categorías distintas registradas para poblar los ComboBox de la interfaz.
    public List<String> obtenerCategorias() throws SQLException {
        String sql = "SELECT DISTINCT categoria FROM catalogo.producto WHERE categoria IS NOT NULL ORDER BY categoria";
        List<String> categorias = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                categorias.add(resultSet.getString("categoria"));
            }
        }

        return categorias;
    }

    // Inserta un nuevo producto validando cada parámetro con PreparedStatement.
    public boolean guardar(Producto producto) throws SQLException {
        String sql = "INSERT INTO catalogo.producto (codigo, nombre, categoria, precio, stock) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, producto.getCodigo());
            statement.setString(2, producto.getNombre());
            statement.setString(3, producto.getCategoria());
            statement.setBigDecimal(4, producto.getPrecio());
            statement.setInt(5, producto.getStock());

            return statement.executeUpdate() > 0;
        }
    }

    // Actualiza un producto existente usando el id como criterio de búsqueda.
    public boolean actualizar(Producto producto) throws SQLException {
        String sql = "UPDATE catalogo.producto SET codigo = ?, nombre = ?, categoria = ?, precio = ?, stock = ? WHERE id_producto = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, producto.getCodigo());
            statement.setString(2, producto.getNombre());
            statement.setString(3, producto.getCategoria());
            statement.setBigDecimal(4, producto.getPrecio());
            statement.setInt(5, producto.getStock());
            statement.setInt(6, producto.getIdProducto());

            return statement.executeUpdate() > 0;
        }
    }

    // Elimina un producto por su identificador único.
    public boolean eliminarPorId(int idProducto) throws SQLException {
        String sql = "DELETE FROM catalogo.producto WHERE id_producto = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idProducto);
            return statement.executeUpdate() > 0;
        }
    }

    // Busca productos por texto, categoría y/o estado del stock.
    public List<Producto> buscar(String texto, String categoria, String stockFiltro) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id_producto, codigo, nombre, categoria, precio, stock FROM catalogo.producto WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();

        if (texto != null && !texto.trim().isEmpty()) {
            sql.append(" AND (codigo ILIKE ? OR nombre ILIKE ?)");
            String valor = "%" + texto.trim() + "%";
            parametros.add(valor);
            parametros.add(valor);
        }

        if (categoria != null && !categoria.isBlank()) {
            sql.append(" AND categoria = ?");
            parametros.add(categoria);
        }

        if (stockFiltro != null && !stockFiltro.isBlank()) {
            switch (stockFiltro) {
                case "Con stock" -> sql.append(" AND stock > 0");
                case "Sin stock" -> sql.append(" AND stock = 0");
                default -> {
                }
            }
        }

        sql.append(" ORDER BY nombre ASC");

        List<Producto> productos = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                statement.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    productos.add(mapearProducto(resultSet));
                }
            }
        }

        return productos;
    }

    // Convierte una fila del ResultSet en un objeto Producto del modelo.
    private Producto mapearProducto(ResultSet resultSet) throws SQLException {
        return new Producto(
                resultSet.getInt("id_producto"),
                resultSet.getString("codigo"),
                resultSet.getString("nombre"),
                resultSet.getString("categoria"),
                resultSet.getBigDecimal("precio"),
                resultSet.getInt("stock")
        );
    }
}
