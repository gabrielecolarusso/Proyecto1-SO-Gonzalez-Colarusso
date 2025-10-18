package so_simulador.planificador;

import so_simulador.modelo.*;

public class Prioridades implements Planificador {

    @Override
    public Proceso seleccionarProceso(ColaProcesos cola) {
        if (cola.estaVacia()) return null;

        Proceso mejor = null;
        ColaProcesos.ColaTemporal aux = cola.crearIterador();

        while (aux.tieneSiguiente()) {
            Proceso actual = aux.siguiente();
            if (mejor == null || actual.getPrioridad() < mejor.getPrioridad()) {
                mejor = actual;
            }
        }

        // Eliminarlo de la cola
        cola.eliminarProceso(mejor);
        return mejor;
    }

    @Override
    public String getNombre() {
        return "Planificación por Prioridades";
    }
}

