package com.appregistros.practicaevaluadacorte2.controller;

import com.appregistros.practicaevaluadacorte2.dao.DataAccessException;
import com.appregistros.practicaevaluadacorte2.dao.ProductoDAO;
import com.appregistros.practicaevaluadacorte2.model.Producto;
import com.appregistros.practicaevaluadacorte2.validacion.ProductoValidator;
import com.appregistros.practicaevaluadacorte2.validacion.ValidacionException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;

// Controlador de la vista de productos.
// Coordina la interacción entre la interfaz JavaFX y la capa DAO.
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
    private final ProductoDAO productoDAO = new ProductoDAO();

    // Inicializa columnas, listas y combos al abrir la vista.
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

    // Carga los productos activos desde la base de datos.
    private void cargarProductos() {
        try {
            productos.setAll(productoDAO.listarActivos());
            tblProductos.setItems(productos);
        } catch (DataAccessException ex) {
            productos.clear();
            mostrarAlerta(Alert.AlertType.ERROR, ex.getMessage());
        }
    }

    // Carga las opciones de categoría y filtro que se muestran en la vista.
    // Las categorías son fijas porque la base solo admite las definidas en chk_categoria.
    private void cargarCombos() {
        cmbCategoria.getItems().setAll(Producto.CATEGORIAS);
        cmbFiltroCategoria.getItems().setAll(Producto.CATEGORIAS);
        cmbFiltroStock.getItems().setAll("Todos", "Con stock", "Sin stock");
    }

    // Guarda un producto nuevo; los datos se validan con ProductoValidator antes de llegar al DAO.
    @FXML
    private void GuardarProducto() {
        Producto producto = leerFormulario();
        if (producto == null) return;

        try {
            boolean guardado = productoDAO.guardar(producto);
            if (guardado) {
                cargarProductos();
                limpiarFormulario();
                mostrarAlerta(Alert.AlertType.INFORMATION, "Producto guardado correctamente.");
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar el producto.");
            }
        } catch (DataAccessException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, ex.getMessage());
        }
    }

    // Actualiza por ID únicamente el producto seleccionado en la tabla, con las mismas validaciones que al guardar.
    @FXML
    private void ActualizarProducto() {
        Producto seleccionado = tblProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Selecciona un producto de la tabla para actualizar.");
            return;
        }

        Producto producto = leerFormulario();
        if (producto == null) return;
        producto.setIdProducto(seleccionado.getIdProducto());

        try {
            boolean actualizado = productoDAO.actualizar(producto);
            if (actualizado) {
               cargarProductos();
               limpiarFormulario();
               tblProductos.getSelectionModel().clearSelection();
               mostrarAlerta(Alert.AlertType.INFORMATION, "Producto actualizado correctamente.");
            } else {
               mostrarAlerta(Alert.AlertType.ERROR, "No se pudo actualizar el producto.");
            }
        } catch (DataAccessException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, ex.getMessage());
        }
    }

    // Elimina el producto seleccionado de la base de datos tras confirmación del usuario.
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
                try {
                    boolean eliminado = productoDAO.eliminarPorId(seleccionado.getIdProducto());
                    if (eliminado) {
                        cargarProductos();
                        limpiarFormulario();
                        tblProductos.getSelectionModel().clearSelection();
                        mostrarAlerta(Alert.AlertType.INFORMATION, "Producto eliminado correctamente.");
                    } else {
                        mostrarAlerta(Alert.AlertType.ERROR, "No se pudo eliminar el producto.");
                    }
                } catch (DataAccessException ex) {
                    mostrarAlerta(Alert.AlertType.ERROR, ex.getMessage());
                }
            }
        });
    }

    // Limpia el formulario para ingresar un nuevo producto.
    @FXML
    private void NuevoProducto() {
        limpiarFormulario();
        tblProductos.getSelectionModel().clearSelection();
    }

    // Aplica filtros de texto, categoría y stock sobre los productos.
    @FXML
    private void Buscar() {
        try {
            String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
            String categoria = cmbFiltroCategoria != null ? cmbFiltroCategoria.getValue() : null;
            String stockFiltro = cmbFiltroStock != null ? cmbFiltroStock.getValue() : null;

            productos.setAll(productoDAO.buscar(texto, categoria, stockFiltro));
            tblProductos.setItems(productos);

            if (productos.isEmpty()) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sin resultados para la búsqueda.");
            }
        } catch (DataAccessException ex) {
            productos.clear();
            mostrarAlerta(Alert.AlertType.ERROR, ex.getMessage());
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

    // Muestra los datos del producto seleccionado en el formulario para edición.

    private void mostrarEnFormulario(Producto producto) {
        if (producto == null) return;
        txtId.setText(String.valueOf(producto.getIdProducto()));
        txtCodigo.setText(producto.getCodigo());
        txtNombre.setText(producto.getNombre());
        cmbCategoria.setValue(producto.getCategoria());
        txtPrecio.setText(String.valueOf(producto.getPrecio()));
        txtStock.setText(String.valueOf(producto.getStock()));
    }

    // Valida el formulario y devuelve el producto normalizado, o null si alguna regla no se cumple
    // (en ese caso se muestra el motivo y no se envía nada a la base de datos).
    private Producto leerFormulario() {
        try {
            return ProductoValidator.validar(txtCodigo.getText(), txtNombre.getText(), cmbCategoria.getValue(),
                    txtPrecio.getText(), txtStock.getText());
        } catch (ValidacionException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, ex.getMessage());
            return null;
        }
    }

    // Muestra una alerta de tipo especificado con el mensaje proporcionado.
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
