package so_simulador.planificador;

import so_simulador.modelo.*;

public class HRRN implements Planificador {

    @Override
    public Proceso seleccionarProceso(ColaProcesos cola) {
        if (cola.estaVacia()) return null;

        Proceso mejor = null;
        double mejorRatio = -1;

        ColaProcesos.ColaTemporal aux = cola.crearIterador();
        while (aux.tieneSiguiente()) {
            Proceso actual = aux.siguiente();

            int servicio = actual.getServicioRestante();
            int espera = actual.getTiempoEspera();

            double ratio = (double) (espera + servicio) / servicio;

            if (ratio > mejorRatio) {
                mejor = actual;
                mejorRatio = ratio;
            }
        }

        // eliminar de la cola
        cola.eliminarProceso(mejor);
        return mejor;
    }

    @Override
    public String getNombre() {
        return "Highest Response Ratio Next (HRRN)";
    }
}
