package com.example.arquitectura_henryolmedo;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;

@RestController
@RequestMapping("/api/figuras")
public class FiguraController {
    private final FiguraFactory fabrica;
    private final CatalogoExterno catalogo;
    public FiguraController(FiguraFactory fabrica, CatalogoExterno catalogo) {
        this.fabrica = fabrica; this.catalogo = catalogo;
    }
    public record Solicitud(String clase, Map<String, Double> parametros) {}
    public record Resultado(String clase, double area, double perimetro) {}
    public record Codigo(String codigo) {}

    @GetMapping public List<String> listar() { return fabrica.disponibles(); }

    @PostMapping("/calcular")
    public Resultado calcular(@RequestBody Solicitud solicitud) {
        if (solicitud.clase() == null) throw new IllegalArgumentException("Indique la clase.");
        Figura figura = fabrica.crear(solicitud.clase(), solicitud.parametros());
        double area = figura.calcularArea(), perimetro = figura.calcularPerimetro();
        if (!Double.isFinite(area) || !Double.isFinite(perimetro) || area <= 0 || perimetro <= 0)
            throw new IllegalArgumentException("La figura produjo un área o perímetro inválido.");
        return new Resultado(solicitud.clase(), area, perimetro);
    }

    @PostMapping("/groovy")
    public Map<String,String> cargar(@RequestBody Codigo solicitud) {
        return Map.of("registrada", fabrica.registrar(solicitud.codigo()));
    }

    @PostMapping("/externas/{nombre}")
    public Map<String,String> importar(@PathVariable String nombre) {
        return Map.of("registrada", fabrica.registrar(catalogo.obtener(nombre)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> errorEntrada(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,String>> errorExterno(IllegalStateException e) {
        return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
    }
}
