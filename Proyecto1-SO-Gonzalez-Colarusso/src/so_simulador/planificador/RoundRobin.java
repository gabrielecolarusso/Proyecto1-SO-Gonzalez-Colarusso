package so_simulador.planificador;

import so_simulador.modelo.*;

public class RoundRobin implements Planificador {
    private int quantum;

    public RoundRobin(int quantum) {
        this.quantum = quantum;
    }

    @Override
    public Proceso seleccionarProceso(ColaProcesos cola) {
        // RR simplemente toma el primer proceso en la cola (FIFO)
        return cola.desencolar();
    }

    @Override
    public String getNombre() {
        return "Round Robin (Quantum=" + quantum + ")";
    }

    public int getQuantum() {
        return quantum;
    }
}
