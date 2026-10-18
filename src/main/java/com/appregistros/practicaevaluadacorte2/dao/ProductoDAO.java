package com.appregistros.practicaevaluadacorte2.dao;

import com.appregistros.practicaevaluadacorte2.Database.DatabaseConnection;
import com.appregistros.practicaevaluadacorte2.model.Producto;
import org.postgresql.util.PSQLException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductoDAO {

    // Se encarga de todas las operaciones de lectura y escritura del catálogo de productos.
    // La capa DAO abstrae la lógica de acceso a datos para que el controlador solo maneje la vista.
    // Toda SQLException se registra (operación + SQLState) y se traduce a DataAccessException con un mensaje seguro.

    private static final Logger LOGGER = Logger.getLogger(ProductoDAO.class.getName());

    // Asigna los parámetros de un PreparedStatement de escritura.
    @FunctionalInterface
    private interface Parametros {
        void asignar(PreparedStatement statement) throws SQLException;
    }

    public List<Producto> listarActivos() throws DataAccessException {
        String sql = "SELECT id_producto, codigo, nombre, categoria, precio, stock FROM catalogo.producto WHERE stock > 0 ORDER BY id_producto";
        List<Producto> productos = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                productos.add(mapearProducto(resultSet));
            }
        } catch (SQLException ex) {
            throw traducir(ex, "SELECT productos activos");
        }

        return productos;
    }

    // Inserta un nuevo producto validando cada parámetro con PreparedStatement.
    public boolean guardar(Producto producto) throws DataAccessException {
        String sql = "INSERT INTO catalogo.producto (codigo, nombre, categoria, precio, stock) VALUES (?, ?, ?, ?, ?)";

        int filas = ejecutarEscritura(sql, statement -> {
            statement.setString(1, producto.getCodigo());
            statement.setString(2, producto.getNombre());
            statement.setString(3, producto.getCategoria());
            statement.setBigDecimal(4, producto.getPrecio());
            statement.setInt(5, producto.getStock());
        }, "INSERT producto codigo=" + producto.getCodigo());

        return filas == 1;
    }

    // Actualiza un producto existente usando el id como criterio de búsqueda.
    // Devuelve false si ninguna fila fue afectada (por ejemplo, si el producto ya fue eliminado).
    public boolean actualizar(Producto producto) throws DataAccessException {
        String sql = "UPDATE catalogo.producto SET codigo = ?, nombre = ?, categoria = ?, precio = ?, stock = ? WHERE id_producto = ?";

        int filas = ejecutarEscritura(sql, statement -> {
            statement.setString(1, producto.getCodigo());
            statement.setString(2, producto.getNombre());
            statement.setString(3, producto.getCategoria());
            statement.setBigDecimal(4, producto.getPrecio());
            statement.setInt(5, producto.getStock());
            statement.setInt(6, producto.getIdProducto());
        }, "UPDATE producto id=" + producto.getIdProducto());

        return filas == 1;
    }

    // Elimina un producto por su identificador único.
    // Devuelve false si ninguna fila fue afectada.
    public boolean eliminarPorId(int idProducto) throws DataAccessException {
        String sql = "DELETE FROM catalogo.producto WHERE id_producto = ?";

        int filas = ejecutarEscritura(sql, statement -> statement.setInt(1, idProducto),
                "DELETE producto id=" + idProducto);

        return filas == 1;
    }

    // Busca productos por texto, categoría y/o estado del stock.
    public List<Producto> buscar(String texto, String categoria, String stockFiltro) throws DataAccessException {
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
        } catch (SQLException ex) {
            throw traducir(ex, "SELECT busqueda texto=" + texto + " categoria=" + categoria + " stock=" + stockFiltro);
        }

        return productos;
    }

    // Ejecuta un INSERT, UPDATE o DELETE dentro de una transacción y devuelve las filas afectadas.
    // Solo confirma si se afectó exactamente una fila; ante cero filas o un error se revierte,
    // de modo que la base nunca queda con un cambio parcial o inesperado.
    private int ejecutarEscritura(String sql, Parametros parametros, String operacion) throws DataAccessException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                parametros.asignar(statement);
                int filas = statement.executeUpdate();

                if (filas == 1) {
                    connection.commit();
                } else {
                    connection.rollback();
                    LOGGER.warning(String.format("Operación=%s | filas afectadas=%d | cambio revertido", operacion, filas));
                }
                return filas;
            } catch (SQLException ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }
                throw ex;
            }
        } catch (SQLException ex) {
            throw traducir(ex, operacion);
        }
    }

    // Registra la operación y el SQLState de la excepción y la convierte en una DataAccessException con mensaje seguro.
    private DataAccessException traducir(SQLException ex, String operacion) {
        String sqlState = ex.getSQLState();
        LOGGER.log(Level.SEVERE, String.format("Operación=%s | SQLState=%s | código=%d | detalle=%s",
                operacion, sqlState, ex.getErrorCode(), ex.getMessage()), ex);
        return new DataAccessException(mensajeSeguro(ex), operacion, sqlState, ex);
    }

    // Devuelve un mensaje comprensible para el usuario según el SQLState, sin exponer detalles internos.
    private String mensajeSeguro(SQLException ex) {
        String sqlState = ex.getSQLState() == null ? "" : ex.getSQLState();

        if (sqlState.startsWith("08")) {
            return "No se pudo conectar con la base de datos. Verifique que PostgreSQL esté en ejecución.";
        }

        return switch (sqlState) {
            case "23505" -> "Ya existe otro producto con ese código.";
            case "23514" -> mensajeCheck(nombreRestriccion(ex));
            case "23502" -> "Hay campos obligatorios sin valor.";
            case "22001" -> "Un texto supera la longitud permitida (código 10 y nombre 80 caracteres).";
            case "22003" -> "El precio o el stock está fuera del rango permitido.";
            case "28P01", "28000" -> "No se pudo iniciar sesión en la base de datos. Revise el usuario y la contraseña.";
            case "3D000" -> "La base de datos configurada no existe.";
            case "42P01", "3F000" -> "La tabla de productos no existe. Ejecute el script de creación de la base de datos.";
            default -> "Ocurrió un error al acceder a la base de datos. Intente de nuevo.";
        };
    }

    // Obtiene el nombre de la restricción violada que informa el servidor PostgreSQL.
    private String nombreRestriccion(SQLException ex) {
        if (ex instanceof PSQLException psqlException && psqlException.getServerErrorMessage() != null) {
            return psqlException.getServerErrorMessage().getConstraint();
        }
        return null;
    }

    // Mensaje para cada restricción CHECK definida en catalogo.producto.
    private String mensajeCheck(String restriccion) {
        return switch (restriccion == null ? "" : restriccion) {
            case "chk_codigo_normalizado" -> "El código debe estar en mayúsculas y sin espacios al inicio o al final.";
            case "chk_nombre_longitud" -> "El nombre debe tener entre 1 y 80 caracteres, sin espacios al inicio o al final.";
            case "chk_precio" -> "El precio debe ser mayor que cero.";
            case "chk_stock" -> "El stock debe ser mayor que cero.";
            case "chk_categoria" -> "La categoría debe ser Papeleria, Tecnologia o Accesorios.";
            default -> "Los datos no cumplen las reglas del catálogo.";
        };
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
