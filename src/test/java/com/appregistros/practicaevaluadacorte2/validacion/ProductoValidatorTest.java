package com.appregistros.practicaevaluadacorte2.validacion;

import com.appregistros.practicaevaluadacorte2.model.Producto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

// Pruebas de las reglas de validación que se aplican al insertar y al actualizar.
class ProductoValidatorTest {

    private static final String CODIGO = "P010";
    private static final String NOMBRE = "Marcador negro";
    private static final String CATEGORIA = "Papeleria";
    private static final String PRECIO = "25.50";
    private static final String STOCK = "10";

    private static String mensajeDeError(String codigo, String nombre, String categoria, String precio, String stock) {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ProductoValidator.validar(codigo, nombre, categoria, precio, stock));
        return ex.getMessage();
    }

    @Test
    @DisplayName("Datos válidos: se normalizan código, nombre y precio")
    void datosValidosSeNormalizan() throws ValidacionException {
        Producto producto = ProductoValidator.validar("  p010 ", "  Marcador negro ", CATEGORIA, "25.5", " 10 ");

        assertEquals("P010", producto.getCodigo());
        assertEquals("Marcador negro", producto.getNombre());
        assertEquals("Papeleria", producto.getCategoria());
        assertEquals(new BigDecimal("25.50"), producto.getPrecio());
        assertEquals(10, producto.getStock());
    }

    @Test
    @DisplayName("Campos obligatorios vacíos o solo con espacios")
    void camposObligatorios() {
        assertTrue(mensajeDeError("", NOMBRE, CATEGORIA, PRECIO, STOCK).contains("código"));
        assertTrue(mensajeDeError(CODIGO, "   ", CATEGORIA, PRECIO, STOCK).contains("nombre"));
        assertTrue(mensajeDeError(CODIGO, NOMBRE, null, PRECIO, STOCK).contains("categoría"));
        assertTrue(mensajeDeError(CODIGO, NOMBRE, CATEGORIA, "", STOCK).contains("precio"));
        assertTrue(mensajeDeError(CODIGO, NOMBRE, CATEGORIA, PRECIO, null).contains("stock"));
    }

    @Test
    @DisplayName("Código: máximo 10 caracteres y sin espacios ni símbolos")
    void codigo() throws ValidacionException {
        assertEquals("ABCDEFGHIJ", ProductoValidator.validar("abcdefghij", NOMBRE, CATEGORIA, PRECIO, STOCK).getCodigo());
        assertTrue(mensajeDeError("ABCDEFGHIJK", NOMBRE, CATEGORIA, PRECIO, STOCK).contains("10 caracteres"));
        assertTrue(mensajeDeError("P 01", NOMBRE, CATEGORIA, PRECIO, STOCK).contains("sin espacios"));
        assertTrue(mensajeDeError("P#01", NOMBRE, CATEGORIA, PRECIO, STOCK).contains("letras, números y guiones"));
    }

    @Test
    @DisplayName("Nombre: entre 1 y 80 caracteres")
    void nombre() throws ValidacionException {
        String ochenta = "N".repeat(80);
        assertEquals(ochenta, ProductoValidator.validar(CODIGO, ochenta, CATEGORIA, PRECIO, STOCK).getNombre());
        assertTrue(mensajeDeError(CODIGO, ochenta + "X", CATEGORIA, PRECIO, STOCK).contains("80 caracteres"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Muebles", "Papelería", "papeleria"})
    @DisplayName("Categoría fuera de la lista permitida")
    void categoriaInvalida(String categoria) {
        assertTrue(mensajeDeError(CODIGO, NOMBRE, categoria, PRECIO, STOCK).contains("categoría válida"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12a", "1e3", "10.", "$10"})
    @DisplayName("Precio no numérico")
    void precioNoNumerico(String precio) {
        assertTrue(mensajeDeError(CODIGO, NOMBRE, CATEGORIA, precio, STOCK).contains("número válido"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-5", "-0.01"})
    @DisplayName("Precio cero o negativo")
    void precioNoPositivo(String precio) {
        assertTrue(mensajeDeError(CODIGO, NOMBRE, CATEGORIA, precio, STOCK).contains("mayor que cero"));
    }

    @Test
    @DisplayName("Precio: máximo 2 decimales y dentro de numeric(10,2)")
    void precioDecimalesYRango() throws ValidacionException {
        assertTrue(mensajeDeError(CODIGO, NOMBRE, CATEGORIA, "10.999", STOCK).contains("2 decimales"));
        assertTrue(mensajeDeError(CODIGO, NOMBRE, CATEGORIA, "100000000", STOCK).contains("no puede ser mayor"));
        assertEquals(new BigDecimal("10.50"), ProductoValidator.validar(CODIGO, NOMBRE, CATEGORIA, "10,5", STOCK).getPrecio());
        assertEquals(new BigDecimal("99999999.99"),
                ProductoValidator.validar(CODIGO, NOMBRE, CATEGORIA, "99999999.99", STOCK).getPrecio());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.5", "abc", "+3", "1e2"})
    @DisplayName("Stock no entero")
    void stockNoEntero(String stock) {
        assertTrue(mensajeDeError(CODIGO, NOMBRE, CATEGORIA, PRECIO, stock).contains("número entero"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    @DisplayName("Stock cero o negativo")
    void stockNoPositivo(String stock) {
        assertTrue(mensajeDeError(CODIGO, NOMBRE, CATEGORIA, PRECIO, stock).contains("mayor que cero"));
    }

    @Test
    @DisplayName("Stock fuera del rango de integer")
    void stockFueraDeRango() {
        assertTrue(mensajeDeError(CODIGO, NOMBRE, CATEGORIA, PRECIO, "99999999999").contains("fuera del rango"));
    }
}
