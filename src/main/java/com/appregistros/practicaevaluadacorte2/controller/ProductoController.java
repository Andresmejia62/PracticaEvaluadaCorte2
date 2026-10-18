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
import java.util.List;

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

    @FXML
    private Label lblContador;

    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private final ProductoDAO productoDAO = new ProductoDAO();

    // Filtros vigentes: los aplicados con el botón Buscar. Se reutilizan al recargar la tabla
    // después de guardar, actualizar o eliminar, para no perder la vista filtrada.
    private String filtroTexto;
    private String filtroCategoria;
    private String filtroStock;

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

        tblProductos.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionado) -> {
                    if (seleccionado != null) mostrarEnFormulario(seleccionado);
                });
        tblProductos.setItems(productos);

        cargarProductos();
    }

    // Consulta la base respetando los filtros vigentes y actualiza la tabla y el contador.
    // Si la consulta falla, la tabla se vacía para no mostrar un listado desactualizado como si fuera correcto.
    // Devuelve true si la consulta se completó.
    private boolean cargarProductos() {
        try {
            List<Producto> resultado = hayFiltrosVigentes()
                    ? productoDAO.buscar(filtroTexto, filtroCategoria, filtroStock)
                    : productoDAO.listarActivos();
            productos.setAll(resultado);
            actualizarContador();
            return true;
        } catch (DataAccessException ex) {
            productos.clear();
            lblContador.setText("No se pudieron cargar los productos.");
            mostrarAlerta(Alert.AlertType.ERROR, ex.getMessage());
            return false;
        }
    }
// Comprueba si hay filtros aplicados para mostrar un mensaje en el contador.
    private boolean hayFiltrosVigentes() {
        return (filtroTexto != null && !filtroTexto.isBlank())
                || (filtroCategoria != null && !filtroCategoria.isBlank())
                || (filtroStock != null && !filtroStock.isBlank() && !"Todos".equals(filtroStock));
    }
// Actualiza el texto del contador de productos mostrados, indicando si hay filtros aplicados.
    private void actualizarContador() {
        String texto = "Productos mostrados: " + productos.size();
        if (hayFiltrosVigentes()) texto += " (con filtros aplicados)";
        lblContador.setText(texto);
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
                limpiarFormulario();
                tblProductos.getSelectionModel().clearSelection();
                cargarProductos();
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
                limpiarFormulario();
                tblProductos.getSelectionModel().clearSelection();
                cargarProductos();
                mostrarAlerta(Alert.AlertType.INFORMATION, "Producto actualizado correctamente.");
            } else {
                // Cero filas afectadas: el producto ya no existe (p. ej. lo eliminó otro usuario).
                cargarProductos();
                mostrarAlerta(Alert.AlertType.WARNING,
                        "No se actualizó ningún producto: el registro seleccionado ya no existe. Se recargó la tabla.");
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

        ButtonType aceptar = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION, "", aceptar, cancelar);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Deseas eliminar el producto " + seleccionado.getCodigo() + " - "
                + seleccionado.getNombre() + "? Esta acción no se puede deshacer.");

        // Cancelar o cerrar la ventana no modifica nada.
        if (confirmacion.showAndWait().orElse(cancelar) != aceptar) return;

        try {
            boolean eliminado = productoDAO.eliminarPorId(seleccionado.getIdProducto());
            if (eliminado) {
                limpiarFormulario();
                tblProductos.getSelectionModel().clearSelection();
                cargarProductos();
                mostrarAlerta(Alert.AlertType.INFORMATION, "Producto eliminado correctamente.");
            } else {
                cargarProductos();
                mostrarAlerta(Alert.AlertType.WARNING,
                        "No se eliminó ningún producto: el registro seleccionado ya no existe. Se recargó la tabla.");
            }
        } catch (DataAccessException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, ex.getMessage());
        }
    }

    // Limpia el formulario para ingresar un nuevo producto.
    @FXML
    private void NuevoProducto() {
        limpiarFormulario();
        tblProductos.getSelectionModel().clearSelection();
    }

    // Aplica filtros de texto, categoría y stock sobre los productos; quedan vigentes hasta limpiarlos.
    @FXML
    private void Buscar() {
        filtroTexto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
        filtroCategoria = cmbFiltroCategoria.getValue();
        filtroStock = cmbFiltroStock.getValue();

        if (cargarProductos() && productos.isEmpty()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sin resultados para la búsqueda.");
        }
    }

    @FXML
    private void LimpiarFiltros() {
        txtBuscar.clear();
        cmbFiltroCategoria.setValue(null);
        cmbFiltroStock.setValue(null);
        filtroTexto = null;
        filtroCategoria = null;
        filtroStock = null;
        cargarProductos();
    }

    // Vuelve a consultar la base manteniendo los filtros vigentes.
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
        txtPrecio.setText(producto.getPrecio() == null ? "" : producto.getPrecio().toPlainString());
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
