import com.example.arquitectura_henryolmedo.Figura

// Pentágono regular. Este archivo se obtiene por HTTP y se compila en ejecución.
class Pentagono extends Figura {
    private final double lado

    Pentagono(Map<String, Double> parametros) {
        if (parametros.keySet() != ['lado'] as Set) {
            throw new IllegalArgumentException('Pentagono requiere únicamente lado')
        }
        lado = parametros.lado
        if (!Double.isFinite(lado) || lado <= 0) throw new IllegalArgumentException('Lado inválido')
    }

    double calcularArea() { 5 * lado * lado / (4 * Math.tan(Math.PI / 5)) }
    double calcularPerimetro() { 5 * lado }
}
