package com.example.coche.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class ManejadorExcepciones {

    @ExceptionHandler(NoResourceFoundException.class)
    public String manejar404() {
        return "redirect:/ping";
    }
}
