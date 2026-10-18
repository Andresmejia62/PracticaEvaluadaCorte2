package com.appregistros.practicaevaluadacorte2.validacion;

// Indica que los datos del formulario no cumplen una regla; el mensaje se muestra tal cual al usuario.
//Realizado con IA y Microsoft
public class ValidacionException extends Exception {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
