package so_simulador.modelo;

public class ColaProcesos {
    private Nodo primero;
    private Nodo ultimo;

    private class Nodo {
        Proceso proceso;
        Nodo siguiente;
        Nodo(Proceso p) { this.proceso = p; }
    }

    public boolean estaVacia() { return primero == null; }

    public void encolar(Proceso p) {
        Nodo nuevo = new Nodo(p);
        if (estaVacia()) {
            primero = ultimo = nuevo;
        } else {
            ultimo.siguiente = nuevo;
            ultimo = nuevo;
        }
    }

    public Proceso desencolar() {
        if (estaVacia()) return null;
        Proceso p = primero.proceso;
        primero = primero.siguiente;
        if (primero == null) ultimo = null;
        return p;
    }

    public void imprimirCola() {
        Nodo actual = primero;
        while (actual != null) {
            System.out.println(actual.proceso);
            actual = actual.siguiente;
        }
    }
}