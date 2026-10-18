package com.appregistros.practicaevaluadacorte2.dao;

// Excepción de la capa de acceso a datos.
// Su mensaje es seguro para mostrarlo al usuario: no expone SQL, nombres internos ni datos de la conexión.
// La SQLException original se conserva como causa y el SQLState queda disponible para el registro.
public class DataAccessException extends Exception {

    private final String operacion;
    private final String sqlState;

    public DataAccessException(String mensajeSeguro, String operacion, String sqlState, Throwable causa) {
        super(mensajeSeguro, causa);
        this.operacion = operacion;
        this.sqlState = sqlState;
    }

    public String getOperacion() {
        return operacion;
    }

    public String getSqlState() {
        return sqlState;
    }
}
