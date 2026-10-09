package com.example.arquitectura_henryolmedo;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FigurasIntegrationTests {
    @LocalServerPort int puerto;
    static HttpServer catalogo;
    static final HttpClient CLIENTE = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) throws Exception {
        catalogo = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        catalogo.createContext("/figuras/Pentagono.groovy", intercambio -> {
            byte[] fuente = Files.readAllBytes(Path.of("../catalogo-externo/figuras/Pentagono.groovy"));
            intercambio.sendResponseHeaders(200, fuente.length);
            try (var salida = intercambio.getResponseBody()) { salida.write(fuente); }
        });
        catalogo.start();
        registry.add("figuras.catalogo-url", () -> "http://127.0.0.1:" + catalogo.getAddress().getPort() + "/figuras/");
    }
    @AfterAll static void apagar() { if (catalogo != null) catalogo.stop(0); }

    HttpResponse<String> post(String ruta, String json) throws Exception {
        return CLIENTE.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + "/api/figuras" + ruta))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
    @Test void calculaCirculoPorHttp() throws Exception {
        var r = post("/calcular", "{\"clase\":\"Circulo\",\"parametros\":{\"radio\":5}}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("78.53981633974483"));
        assertTrue(r.body().contains("31.41592653589793"));
    }
    @Test void importaDesdeOtroServidorYCalculaSinReiniciar() throws Exception {
        assertEquals(200, post("/externas/Pentagono", "{}").statusCode());
        var r = post("/calcular", "{\"clase\":\"Pentagono\",\"parametros\":{\"lado\":5}}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("43.0119"));
        assertTrue(r.body().contains("25.0"));
        assertEquals(400, post("/externas/Pentagono", "{}").statusCode());
    }
    @Test void erroresEntradaYCatalogo() throws Exception {
        assertEquals(400, post("/calcular", "{\"clase\":\"Circulo\",\"parametros\":{\"radio\":-1}}").statusCode());
        assertEquals(400, post("/calcular", "{\"clase\":\"Desconocida\",\"parametros\":{\"lado\":1}}").statusCode());
        assertEquals(400, post("/calcular", "{\"clase\":\"Circulo\",\"parametros\":{\"lado\":1}}").statusCode());
        assertEquals(502, post("/externas/NoExiste", "{}").statusCode());
        assertEquals(400, post("/groovy", "{\"codigo\":\"class SinFigura {}\"}").statusCode());
        assertEquals(400, post("/groovy", "{\"codigo\":\"esto no es codigo {{{\"}").statusCode());
    }
    @Test void figurasOriginalesMantienenSusFormulas() {
        FiguraFactory f = new FiguraFactory();
        Figura c = f.crear("Cuadrado", Map.of("lado",4d));
        assertEquals(16, c.calcularArea()); assertEquals(16, c.calcularPerimetro());
        Figura t = f.crear("Triangulo", Map.of("base",3d,"altura",4d,"lado1",3d,"lado2",4d,"lado3",5d));
        assertEquals(6,t.calcularArea()); assertEquals(12,t.calcularPerimetro());
    }
    @Test void compilaFiguraLocalYRechazaContratoInvalido() throws Exception {
        FiguraFactory f = new FiguraFactory();
        f.registrar(Files.readString(Path.of("../catalogo-externo/figuras/Hexagono.groovy")));
        Figura h = f.crear("Hexagono", Map.of("lado", 2d));
        assertEquals(6 * Math.sqrt(3), h.calcularArea(), 1e-9);
        assertEquals(12, h.calcularPerimetro());
        assertThrows(IllegalArgumentException.class, () -> f.crear("Hexagono", Map.of("radio",2d)));
        assertThrows(IllegalArgumentException.class, () -> f.crear("Circulo", Map.of("radio",Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> f.registrar("class Mala extends com.example.arquitectura_henryolmedo.Figura { double calcularArea(){1}; double calcularPerimetro(){1} }"));
    }
}
