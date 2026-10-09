import com.example.arquitectura_henryolmedo.Figura

class Hexagono extends Figura {
    private final double lado
    Hexagono(Map<String, Double> parametros) {
        if (parametros.keySet() != ['lado'] as Set) throw new IllegalArgumentException('Hexagono requiere únicamente lado')
        lado = parametros.lado
        if (!Double.isFinite(lado) || lado <= 0) throw new IllegalArgumentException('Lado inválido')
    }
    double calcularArea() { 3 * Math.sqrt(3) * lado * lado / 2 }
    double calcularPerimetro() { 6 * lado }
}
