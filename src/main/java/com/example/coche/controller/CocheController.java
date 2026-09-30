package com.example.coche.controller;

import com.example.coche.exception.CocheNoEncontradoException;
import com.example.coche.model.Coche;
import com.example.coche.service.CocheService;
import com.example.coche.service.ResultadoPagina;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/coches")
public class CocheController {

    private static final int COCHES_POR_PAGINA = 6;

    private final CocheService cocheService;

    public CocheController(CocheService cocheService) {
        this.cocheService = cocheService;
    }

    @GetMapping
    public String listar(@RequestParam(defaultValue = "0") int pagina,
                          @RequestParam(required = false) String marca,
                          @RequestParam(required = false) String combustible,
                          @RequestParam(required = false) String transmision,
                          Model model) {

        ResultadoPagina<Coche> resultado =
                cocheService.buscar(marca, combustible, transmision, pagina, COCHES_POR_PAGINA);

        model.addAttribute("coches", resultado.getContenido());
        model.addAttribute("paginaActual", resultado.getPaginaActual());
        model.addAttribute("totalPaginas", resultado.getTotalPaginas());
        model.addAttribute("totalElementos", resultado.getTotalElementos());
        model.addAttribute("marca", marca);
        model.addAttribute("combustible", combustible);
        model.addAttribute("transmision", transmision);
        model.addAttribute("filtrosActivos", contarFiltrosActivos(marca, combustible, transmision));
        model.addAttribute("marcasDisponibles", cocheService.listarMarcasDisponibles());
        model.addAttribute("combustiblesDisponibles", cocheService.listarCombustiblesDisponibles());
        model.addAttribute("transmisionesDisponibles", cocheService.listarTransmisionesDisponibles());
        return "coches/list";
    }

    private int contarFiltrosActivos(String marca, String combustible, String transmision) {
        int contador = 0;
        if (marca != null && !marca.isBlank()) contador++;
        if (combustible != null && !combustible.isBlank()) contador++;
        if (transmision != null && !transmision.isBlank()) contador++;
        return contador;
    }

    @GetMapping("/nuevo")
    public String nuevoFormulario(Model model) {
        model.addAttribute("coche", new Coche());
        return "coches/form";
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable Long id, Model model) {
        model.addAttribute("coche", cocheService.findById(id));
        return "coches/view";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("coche") Coche coche,
                         BindingResult resultado,
                         RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return "coches/form";
        }
        cocheService.save(coche);
        redirectAttributes.addFlashAttribute("mensaje", "Coche creado correctamente");
        return "redirect:/coches";
    }

    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable Long id, Model model) {
        model.addAttribute("coche", cocheService.findById(id));
        return "coches/form";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                              @Valid @ModelAttribute("coche") Coche coche,
                              BindingResult resultado,
                              RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return "coches/form";
        }
        cocheService.update(id, coche);
        redirectAttributes.addFlashAttribute("mensaje", "Coche actualizado correctamente");
        return "redirect:/coches";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        cocheService.deleteById(id);
        redirectAttributes.addFlashAttribute("mensaje", "Coche eliminado correctamente");
        return "redirect:/coches";
    }

    @ExceptionHandler(CocheNoEncontradoException.class)
    public String cocheNoEncontrado(CocheNoEncontradoException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/coches";
    }
}
