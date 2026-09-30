package com.example.coche.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class ErrorController {

    @ExceptionHandler(NoResourceFoundException.class)
    public String manejar404() {
        return "redirect:/ping";
    }
}
