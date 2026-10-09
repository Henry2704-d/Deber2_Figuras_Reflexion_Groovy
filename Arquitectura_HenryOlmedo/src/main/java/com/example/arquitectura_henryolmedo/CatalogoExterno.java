package com.example.arquitectura_henryolmedo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class CatalogoExterno {
    private final String base;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();

    public CatalogoExterno(@Value("${figuras.catalogo-url}") String base) {
        this.base = base.endsWith("/") ? base : base + "/";
    }

    public String obtener(String nombre) {
        if (nombre == null || !nombre.matches("[A-Za-z][A-Za-z0-9_]{0,63}"))
            throw new IllegalArgumentException("Nombre de figura inválido.");
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(base + nombre + ".groovy"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() != 200)
                throw new IllegalStateException("El catálogo respondió HTTP " + respuesta.statusCode());
            if (respuesta.body().length() > 65536)
                throw new IllegalStateException("La figura externa excede el tamaño permitido.");
            return respuesta.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta al catálogo interrumpida.", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo conectar al catálogo externo.", e);
        }
    }
}
