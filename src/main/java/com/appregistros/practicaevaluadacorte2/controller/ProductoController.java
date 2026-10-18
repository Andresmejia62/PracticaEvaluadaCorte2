package com.appregistros.practicaevaluadacorte2.controller;

import com.appregistros.practicaevaluadacorte2.Database.DatabaseConnection;
import com.appregistros.practicaevaluadacorte2.model.Producto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductoController {
    @FXML
    private TextField txtId, txtCodigo, txtNombre, txtPrecio, txtStock;
    @FXML
    private ComboBox<String> cmbCategoria;

    @FXML
    private TextField txtBuscar;
    @FXML
    private ComboBox<String> cmbFiltroCategoria, cmbFiltroStock;

    @FXML
    private TableView<Producto> tblProductos;
    @FXML
    private TableColumn<Producto, Integer> colId, colStock;
    @FXML
    private TableColumn<Producto, String> colCodigo, colNombre, colCategoria;
    @FXML
    private TableColumn<Producto, BigDecimal> colPrecio;

    private final ObservableList<Producto> productos = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colId.setCellValueFactory(cellData -> cellData.getValue().idProductoProperty().asObject());
        colCodigo.setCellValueFactory(cellData -> cellData.getValue().codigoProperty());
        colNombre.setCellValueFactory(cellData -> cellData.getValue().nombreProperty());
        colCategoria.setCellValueFactory(cellData -> cellData.getValue().categoriaProperty());
        colPrecio.setCellValueFactory(cellData -> cellData.getValue().precioProperty());
        colStock.setCellValueFactory(cellData -> cellData.getValue().stockProperty().asObject());
        cargarCombos();
        cargarProductos();


        tblProductos.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionado) -> {
                    if (seleccionado != null) mostrarEnFormulario(seleccionado);
                });
        tblProductos.setItems(productos);
    }

    private void cargarProductos() {
        System.out.println("CargarProductos invoked");
        productos.clear();

        String sql = "SELECT id_producto, codigo, nombre, categoria, precio, stock FROM catalogo.producto WHERE stock > 0 ORDER BY id_producto";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                Producto producto = new Producto(
                        resultSet.getInt("id_producto"),
                        resultSet.getString("codigo"),
                        resultSet.getString("nombre"),
                        resultSet.getString("categoria"),
                        resultSet.getBigDecimal("precio"),
                        resultSet.getInt("stock")
                );
                productos.add(producto);
            }

            tblProductos.setItems(productos);

        } catch (SQLException ex) {
            System.out.println("error al cargar Productos: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void cargarCombos() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            cargarCombo(connection, "SELECT DISTINCT categoria FROM catalogo.producto WHERE categoria IS NOT NULL ORDER BY categoria", cmbCategoria);
            cargarCombo(connection, "SELECT DISTINCT categoria FROM catalogo.producto WHERE categoria IS NOT NULL ORDER BY categoria", cmbFiltroCategoria);
            cmbFiltroStock.getItems().setAll("Todos", "Con stock", "Sin stock");
        } catch (SQLException ex) {
            System.out.println("error al cargar combos: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void cargarCombo(Connection connection, String sql, ComboBox<String> comboBox) throws SQLException {
        comboBox.getItems().clear();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                comboBox.getItems().add(resultSet.getString(1));
            }
        }
    }

    @FXML
    private void GuardarProducto() {
        System.out.println("GuardarProducto invoked");

        String codigo = txtCodigo.getText() == null ? "" : txtCodigo.getText().trim();
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String categoria = cmbCategoria.getValue();
        String precioTexto = txtPrecio.getText() == null ? "" : txtPrecio.getText().trim();
        String stockTexto = txtStock.getText() == null ? "" : txtStock.getText().trim();

        if (codigo.isEmpty() || nombre.isEmpty() || categoria == null || categoria.isBlank()
                || precioTexto.isEmpty() || stockTexto.isEmpty()) {
            System.out.println("Completa todos los campos");
            return;
        }

        String sql = "INSERT INTO catalogo.producto (codigo, nombre, categoria, precio, stock) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, codigo);
            preparedStatement.setString(2, nombre);
            preparedStatement.setString(3, categoria);
            preparedStatement.setBigDecimal(4, new BigDecimal(precioTexto));
            preparedStatement.setInt(5, Integer.parseInt(stockTexto));

            int filas = preparedStatement.executeUpdate();

            if (filas > 0) {
                System.out.println("Producto guardado correctamente");
                cargarProductos();
                limpiarFormulario();
            }

        } catch (SQLException ex) {
            System.out.println("error al guardar Producto: " + ex.getMessage());
            ex.printStackTrace();
        } catch (NumberFormatException ex) {
            System.out.println("Precio o stock inválidos");
        }
    }

    @FXML
    private void ActualizarProducto() {
        if (tblProductos.getSelectionModel().getSelectedItem() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selecciona un producto de la tabla.");
            return;
        }
        if (!validarFormulario()) return;

        String sql = "UPDATE catalogo.producto SET codigo = ?, nombre = ?, categoria = ?, precio = ?, stock = ? WHERE id_producto = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, txtCodigo.getText().trim());
            preparedStatement.setString(2, txtNombre.getText().trim());
            preparedStatement.setString(3, cmbCategoria.getValue());
            preparedStatement.setBigDecimal(4, new BigDecimal(txtPrecio.getText().trim()));
            preparedStatement.setInt(5, Integer.parseInt(txtStock.getText().trim()));
            preparedStatement.setInt(6, Integer.parseInt(txtId.getText().trim()));

            int filas = preparedStatement.executeUpdate();

            if (filas > 0) {
                System.out.println("Producto actualizado correctamente");
                cargarProductos();
                limpiarFormulario();
                tblProductos.getSelectionModel().clearSelection();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "No se pudo actualizar el producto.");
            }

        } catch (SQLException ex) {
            System.out.println("error al actualizar Producto: " + ex.getMessage());
            ex.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error al actualizar el producto: " + ex.getMessage());
        } catch (NumberFormatException ex) {
            System.out.println("ID, precio o stock inválidos");
            mostrarAlerta(Alert.AlertType.ERROR, "ID, precio o stock inválidos.");
        }
    }

    @FXML
    private void EliminarProducto() {
        Producto seleccionado = tblProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selecciona un producto de la tabla para eliminar.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Deseas eliminar el producto " + seleccionado.getNombre() + "?");

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.OK) {
                String sql = "DELETE FROM catalogo.producto WHERE id_producto = ?";

                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

                    preparedStatement.setInt(1, seleccionado.getIdProducto());
                    int filas = preparedStatement.executeUpdate();

                    if (filas > 0) {
                        System.out.println("Producto eliminado correctamente");
                        cargarProductos();
                        limpiarFormulario();
                        tblProductos.getSelectionModel().clearSelection();
                    } else {
                        mostrarAlerta(Alert.AlertType.ERROR, "No se pudo eliminar el producto.");
                    }

                } catch (SQLException ex) {
                    System.out.println("error al eliminar Producto: " + ex.getMessage());
                    ex.printStackTrace();
                    mostrarAlerta(Alert.AlertType.ERROR, "Error al eliminar el producto: " + ex.getMessage());
                }
            }
        });
    }

    @FXML
    private void NuevoProducto() {
        limpiarFormulario();
        tblProductos.getSelectionModel().clearSelection();
    }

    private Producto crearProductoDesdeResultSet(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getInt("id_producto"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("categoria"),
                rs.getBigDecimal("precio"),
                rs.getInt("stock")
        );
    }

    @FXML
    private void Buscar() {
        productos.clear();

        StringBuilder sql = new StringBuilder("""
                SELECT id_producto, codigo, nombre, categoria, precio, stock
                FROM catalogo.producto
                WHERE 1 = 1
                """);

        List<Object> parametros = new ArrayList<>();

        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
        if (!texto.isBlank()) {
            sql.append("""
                    AND (
                        codigo ILIKE ?
                        OR nombre ILIKE ?
                    )
                    """);
            parametros.add("%" + texto + "%");
            parametros.add("%" + texto + "%");
        }

        String categoria = cmbFiltroCategoria != null ? cmbFiltroCategoria.getValue() : null;
        if (categoria != null && !categoria.isBlank()) {
            sql.append(" AND categoria = ? ");
            parametros.add(categoria);
        }

        String stockFiltro = cmbFiltroStock != null ? cmbFiltroStock.getValue() : null;
        if (stockFiltro != null && !stockFiltro.isBlank()) {
            switch (stockFiltro) {
                case "Con stock" -> sql.append(" AND stock > 0 ");
                case "Sin stock" -> sql.append(" AND stock = 0 ");
                default -> {
                }
            }
        }

        sql.append(" ORDER BY nombre ASC ");

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                statement.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    productos.add(crearProductoDesdeResultSet(resultSet));
                }
            }

            tblProductos.setItems(productos);

            if (productos.isEmpty()) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sin resultados para la búsqueda.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo realizar la búsqueda.");
        }
    }

    @FXML
    private void LimpiarFiltros() {
        txtBuscar.clear();
        cmbFiltroCategoria.setValue(null);
        cmbFiltroStock.setValue(null);
        cargarProductos();
    }

    @FXML
    private void Recargar() {

        cargarProductos();
    }

    private void mostrarEnFormulario(Producto producto) {
        if (producto == null) return;
        txtId.setText(String.valueOf(producto.getIdProducto()));
        txtCodigo.setText(producto.getCodigo());
        txtNombre.setText(producto.getNombre());
        cmbCategoria.setValue(producto.getCategoria());
        txtPrecio.setText(String.valueOf(producto.getPrecio()));
        txtStock.setText(String.valueOf(producto.getStock()));
    }

    private boolean validarFormulario() {
        String codigo = txtCodigo.getText() == null ? "" : txtCodigo.getText().trim();
        String nombre = txtNombre.getText() == null ? "" : txtNombre.getText().trim();
        String categoria = cmbCategoria.getValue();
        String precioTexto = txtPrecio.getText() == null ? "" : txtPrecio.getText().trim();
        String stockTexto = txtStock.getText() == null ? "" : txtStock.getText().trim();

        if (codigo.isEmpty() || nombre.isEmpty() || categoria == null || categoria.isBlank()
                || precioTexto.isEmpty() || stockTexto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Completa todos los campos antes de actualizar.");
            return false;
        }

        try {
            new BigDecimal(precioTexto);
            Integer.parseInt(stockTexto);
        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Precio o stock inválidos.");
            return false;
        }

        return true;
    }

    private void mostrarAlerta(Alert.AlertType tipo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void limpiarFormulario() {
        txtId.clear();
        txtCodigo.clear();
        txtNombre.clear();
        txtPrecio.clear();
        txtStock.clear();
        cmbCategoria.setValue(null);
    }

}
