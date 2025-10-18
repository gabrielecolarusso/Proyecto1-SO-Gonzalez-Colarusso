package so_simulador.planificador;

import so_simulador.modelo.*;

public class SJF implements Planificador {

    @Override
    public Proceso seleccionarProceso(ColaProcesos cola) {
        if (cola.estaVacia()) return null;

        // Recorremos la cola para encontrar el proceso con menos instrucciones
        Proceso masCorto = null;
        ColaProcesos.ColaTemporal aux = cola.crearIterador();

        while (aux.tieneSiguiente()) {
            Proceso actual = aux.siguiente();
            if (masCorto == null || actual.getInstruccionesTotales() < masCorto.getInstruccionesTotales()) {
                masCorto = actual;
            }
        }

        // Eliminamos ese proceso específico de la cola
        cola.eliminarProceso(masCorto);
        return masCorto;
    }

    @Override
    public String getNombre() {
        return "Shortest Job First (SJF)";
    }
}