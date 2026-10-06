package com.example.coche.controller;

import com.example.coche.repository.LogTransaccionDAO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// @RestController: lo que devuelve el método no es el nombre de una vista, sino datos que Spring convierte a JSON
@RestController
public class LogController {

    private final LogTransaccionDAO logTransaccionDAO;

    public LogController(LogTransaccionDAO logTransaccionDAO) {
        this.logTransaccionDAO = logTransaccionDAO;
    }

    @GetMapping("/log")
    public List<Map<String, Object>> verLog() {
        return logTransaccionDAO.listarTodos();
    }
}
