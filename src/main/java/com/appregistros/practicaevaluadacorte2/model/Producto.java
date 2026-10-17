package com.appregistros.practicaevaluadacorte2.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;

public class Producto {
    private final IntegerProperty idProducto = new SimpleIntegerProperty(this, "idProducto");
    private final StringProperty codigo = new SimpleStringProperty(this, "codigo");
    private final StringProperty nombre = new SimpleStringProperty(this, "nombre");
    private final StringProperty categoria = new SimpleStringProperty(this, "categoria");
    private final ObjectProperty<BigDecimal> precio = new SimpleObjectProperty<>(this, "precio");
    private final IntegerProperty stock = new SimpleIntegerProperty(this, "stock");

    public Producto() {
    }

    public Producto(int id_producto, String codigo, String nombre, String categoria, BigDecimal precio, int stock) {
        setIdProducto(id_producto);
        setCodigo(codigo);
        setNombre(nombre);
        setCategoria(categoria);
        setPrecio(precio);
        setStock(stock);
    }

    public int getIdProducto() {
        return idProducto.get();
    }

    public void setIdProducto(int idProducto) {
        this.idProducto.set(idProducto);
    }

    public IntegerProperty idProductoProperty() {
        return idProducto;
    }

    public String getCodigo() {
        return codigo.get();
    }

    public void setCodigo(String codigo) {
        this.codigo.set(codigo);
    }

    public StringProperty codigoProperty() {
        return codigo;
    }

    public String getNombre() {
        return nombre.get();
    }

    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public String getCategoria() {
        return categoria.get();
    }

    public void setCategoria(String categoria) {
        this.categoria.set(categoria);
    }

    public StringProperty categoriaProperty() {
        return categoria;
    }

    public BigDecimal getPrecio() {
        return precio.get();
    }

    public void setPrecio(BigDecimal precio) {
        this.precio.set(precio);
    }

    public ObjectProperty<BigDecimal> precioProperty() {
        return precio;
    }

    public int getStock() {
        return stock.get();
    }

    public void setStock(int stock) {
        this.stock.set(stock);
    }

    public IntegerProperty stockProperty() {
        return stock;
    }
}
