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

    public class ColaTemporal {
        private Nodo cursor;
        ColaTemporal(Nodo inicio) { this.cursor = inicio; }
        public boolean tieneSiguiente() { return cursor != null; }
        public Proceso siguiente() {
            Proceso p = cursor.proceso;
            cursor = cursor.siguiente;
            return p;
        }
    }

    public ColaTemporal crearIterador() {
        return new ColaTemporal(primero);
    }

    public void eliminarProceso(Proceso p) {
        if (estaVacia()) return;

        if (primero.proceso == p) {
            desencolar();
            return;
        }

        Nodo actual = primero;
        while (actual.siguiente != null) {
            if (actual.siguiente.proceso == p) {
                actual.siguiente = actual.siguiente.siguiente;
                if (actual.siguiente == null) {
                    ultimo = actual;
                }
                return;
            }
            actual = actual.siguiente;
        }
    }
}