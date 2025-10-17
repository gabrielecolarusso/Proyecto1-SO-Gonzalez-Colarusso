package so_simulador.planificador;

import so_simulador.modelo.*;

public class FCFS implements Planificador {

    @Override
    public Proceso seleccionarProceso(ColaProcesos cola) {
        // FCFS toma el primer proceso en la cola (FIFO)
        return cola.desencolar();
    }

    @Override
    public String getNombre() {
        return "First Come, First Served (FCFS)";
    }
}