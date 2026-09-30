package com.example.coche.exception;

public class CocheNoEncontradoException extends RuntimeException {

    public CocheNoEncontradoException(Long id) {
        super("No existe ningún coche con id " + id);
    }
}
