package com.example.arquitectura_henryolmedo;

import groovy.lang.GroovyClassLoader;
import org.springframework.stereotype.Service;
import java.lang.reflect.*;
import java.util.*;

/** Registro común para figuras Java y Groovy. Nunca usa el nombre recibido para cargar clases arbitrarias. */
@Service
public class FiguraFactory {
    private final Map<String, Class<? extends Figura>> registro = new TreeMap<>();

    public FiguraFactory() {
        registro.put("Circulo", Circulo.class);
        registro.put("Cuadrado", Cuadrado.class);
        registro.put("Triangulo", Triangulo.class);
    }

    public synchronized List<String> disponibles() { return List.copyOf(registro.keySet()); }

    public synchronized String registrar(String codigo) {
        if (codigo == null || codigo.isBlank() || codigo.length() > 65536)
            throw new IllegalArgumentException("Envíe código Groovy de entre 1 y 65536 caracteres.");
        // Código de confianza: GroovyClassLoader compila, pero NO constituye una sandbox.
        try (GroovyClassLoader loader = new GroovyClassLoader(Figura.class.getClassLoader())) {
            Class<?> tipo = loader.parseClass(codigo);
            if (!Figura.class.isAssignableFrom(tipo) || Modifier.isAbstract(tipo.getModifiers()))
                throw new IllegalArgumentException("La clase debe ser concreta y extender Figura.");
            tipo.getConstructor(Map.class);
            String nombre = tipo.getSimpleName();
            if (registro.containsKey(nombre))
                throw new IllegalArgumentException("La figura ya existe: " + nombre);
            registro.put(nombre, tipo.asSubclass(Figura.class));
            return nombre;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo compilar/registrar: " + e.getMessage(), e);
        }
    }

    public synchronized Figura crear(String nombre, Map<String, Double> parametros) {
        Class<? extends Figura> tipo = registro.get(nombre);
        if (tipo == null) throw new IllegalArgumentException("Figura no registrada: " + nombre);
        if (parametros == null || parametros.isEmpty())
            throw new IllegalArgumentException("Indique los parámetros de la figura.");
        parametros.forEach((k,v) -> {
            if (v == null || !Double.isFinite(v) || v <= 0)
                throw new IllegalArgumentException("El parámetro " + k + " debe ser positivo y finito.");
        });
        try {
            // Contrato de extensiones: constructor público con Map de parámetros.
            try { return tipo.getConstructor(Map.class).newInstance(Map.copyOf(parametros)); }
            catch (NoSuchMethodException ignored) { /* Figuras Java del deber anterior. */ }
            Constructor<?> ctor = tipo.getConstructors()[0];
            Parameter[] requeridos = ctor.getParameters();
            Set<String> nombres = new HashSet<>();
            Object[] valores = new Object[requeridos.length];
            for (int i = 0; i < requeridos.length; i++) {
                String clave = requeridos[i].getName();
                nombres.add(clave);
                if (!parametros.containsKey(clave)) throw new IllegalArgumentException("Falta el parámetro: " + clave);
                valores[i] = parametros.get(clave);
            }
            if (!nombres.equals(parametros.keySet()))
                throw new IllegalArgumentException("Los parámetros esperados son: " + nombres);
            return (Figura) ctor.newInstance(valores);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Parámetros inválidos: " + e.getCause().getMessage(), e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("No se pudo instanciar la figura.", e);
        }
    }
}
