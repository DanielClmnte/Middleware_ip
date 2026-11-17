package com.example.coche.exception;

// Se lanza cuando los datos de un coche no son validos (campo vacio, año absurdo, matricula repetida...)
public class DatosInvalidosException extends RuntimeException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
