package com.appregistros.practicaevaluadacorte2.validacion;

import com.appregistros.practicaevaluadacorte2.model.Producto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// Valida y normaliza los datos del formulario antes de enviarlos al DAO.
// Las reglas reflejan las restricciones de catalogo.producto para que ningún dato inválido llegue a PostgreSQL.
// Se usa igual al insertar y al actualizar.

//Realizado con IA y Microsoft
public final class ProductoValidator {

    public static final int MAX_CODIGO = 10;
    public static final int MAX_NOMBRE = 80;
    public static final int MAX_DECIMALES_PRECIO = 2;
    // numeric(10,2): ocho dígitos enteros y dos decimales.
    public static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");

    private static final Pattern PATRON_CODIGO = Pattern.compile("[\\p{Lu}0-9-]+");
    private static final Pattern PATRON_DECIMAL = Pattern.compile("-?\\d+([.,]\\d+)?");
    private static final Pattern PATRON_ENTERO = Pattern.compile("-?\\d+");

    private ProductoValidator() {
    }

    // Devuelve un Producto (sin id) con los datos normalizados, o lanza ValidacionException
    // con un mensaje pertinente para la primera regla que no se cumple.
    public static Producto validar(String codigoTexto, String nombreTexto, String categoria,
                                   String precioTexto, String stockTexto) throws ValidacionException {
        String codigo = limpiar(codigoTexto).toUpperCase(Locale.ROOT);
        String nombre = limpiar(nombreTexto);
        String categoriaLimpia = limpiar(categoria);
        String precioLimpio = limpiar(precioTexto);
        String stockLimpio = limpiar(stockTexto);

        List<String> faltantes = new ArrayList<>();
        if (codigo.isEmpty()) faltantes.add("código");
        if (nombre.isEmpty()) faltantes.add("nombre");
        if (categoriaLimpia.isEmpty()) faltantes.add("categoría");
        if (precioLimpio.isEmpty()) faltantes.add("precio");
        if (stockLimpio.isEmpty()) faltantes.add("stock");
        if (!faltantes.isEmpty()) {
            throw new ValidacionException("Completa los campos obligatorios: " + String.join(", ", faltantes) + ".");
        }

        Producto producto = new Producto();
        producto.setCodigo(validarCodigo(codigo));
        producto.setNombre(validarNombre(nombre));
        producto.setCategoria(validarCategoria(categoriaLimpia));
        producto.setPrecio(validarPrecio(precioLimpio));
        producto.setStock(validarStock(stockLimpio));
        return producto;
    }

    private static String validarCodigo(String codigo) throws ValidacionException {
        if (codigo.length() > MAX_CODIGO) {
            throw new ValidacionException("El código no puede tener más de " + MAX_CODIGO + " caracteres.");
        }
        if (!PATRON_CODIGO.matcher(codigo).matches()) {
            throw new ValidacionException("El código solo puede contener letras, números y guiones, sin espacios.");
        }
        return codigo;
    }

    private static String validarNombre(String nombre) throws ValidacionException {
        if (nombre.length() > MAX_NOMBRE) {
            throw new ValidacionException("El nombre no puede tener más de " + MAX_NOMBRE + " caracteres.");
        }
        return nombre;
    }

    private static String validarCategoria(String categoria) throws ValidacionException {
        if (!Producto.CATEGORIAS.contains(categoria)) {
            throw new ValidacionException("Selecciona una categoría válida: " + String.join(", ", Producto.CATEGORIAS) + ".");
        }
        return categoria;
    }

    private static BigDecimal validarPrecio(String precioTexto) throws ValidacionException {
        if (!PATRON_DECIMAL.matcher(precioTexto).matches()) {
            throw new ValidacionException("El precio debe ser un número válido, por ejemplo 65.00.");
        }

        BigDecimal precio = new BigDecimal(precioTexto.replace(',', '.'));
        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException("El precio debe ser mayor que cero.");
        }
        if (precio.stripTrailingZeros().scale() > MAX_DECIMALES_PRECIO) {
            throw new ValidacionException("El precio admite como máximo " + MAX_DECIMALES_PRECIO + " decimales.");
        }
        if (precio.compareTo(PRECIO_MAXIMO) > 0) {
            throw new ValidacionException("El precio no puede ser mayor que " + PRECIO_MAXIMO.toPlainString() + ".");
        }
        return precio.setScale(MAX_DECIMALES_PRECIO, RoundingMode.UNNECESSARY);
    }

    private static int validarStock(String stockTexto) throws ValidacionException {
        if (!PATRON_ENTERO.matcher(stockTexto).matches()) {
            throw new ValidacionException("El stock debe ser un número entero, sin decimales.");
        }

        int stock;
        try {
            stock = Integer.parseInt(stockTexto);
        } catch (NumberFormatException ex) {
            throw new ValidacionException("El stock está fuera del rango permitido.");
        }

        if (stock <= 0) {
            throw new ValidacionException("El stock debe ser mayor que cero.");
        }
        return stock;
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
