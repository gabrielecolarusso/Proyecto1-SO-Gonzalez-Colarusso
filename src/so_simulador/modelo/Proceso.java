package so_simulador.modelo;

public class Proceso implements Runnable {
    private PCB pcb;
    private int instruccionesTotales;
    private boolean esCPUbound;
    private int ciclosExcepcion;
    private int ciclosAtencion;
    private int prioridad; 
    private int tiempoLlegada;
    private int tiempoEspera;

    public Proceso(String nombre, int instrucciones, boolean esCPUbound, int ciclosExcepcion, int ciclosAtencion, int prioridad) {
        this.pcb = new PCB(nombre);
        this.instruccionesTotales = instrucciones;
        this.esCPUbound = esCPUbound;
        this.ciclosExcepcion = ciclosExcepcion;
        this.ciclosAtencion = ciclosAtencion;
        this.prioridad = prioridad;
        this.tiempoLlegada = 0; 
        this.tiempoEspera = 0;
    }

    public PCB getPCB() { return pcb; }
    public int getInstruccionesTotales() { return instruccionesTotales; }
    public boolean isCPUbound() { return esCPUbound; }
    public int getCiclosExcepcion() { return ciclosExcepcion; }
    public int getCiclosAtencion() { return ciclosAtencion; }
    public int getPrioridad() { return prioridad; }
    public void setTiempoLlegada(int ciclo) { this.tiempoLlegada = ciclo; }
    public int getTiempoLlegada() { return tiempoLlegada; }

    // 🔹 Control de espera para HRRN
    public void incrementarEspera() { this.tiempoEspera++; }
    public int getTiempoEspera() { return tiempoEspera; }
    public void resetEspera() { this.tiempoEspera = 0; }

    // 🔹 Servicio restante (para HRRN)
    public int getServicioRestante() {
        return instruccionesTotales - pcb.getProgramCounter();
    }

    /**
     * Ejecución completa (usado en FCFS, SJF, HRRN, Prioridades).
     */
    @Override
    public void run() {
        pcb.setEstado(EstadoProceso.EJECUCION);
        for (int i = pcb.getProgramCounter(); i < instruccionesTotales; i++) {
            pcb.incrementarPC();
            pcb.incrementarMAR();
            // Aquí luego meteremos lógica de excepciones y semáforos
        }
        pcb.setEstado(EstadoProceso.TERMINADO);
    }

    /**
     * Ejecución parcial por quantum (usado en Round Robin).
     * @param quantum cantidad de instrucciones a ejecutar
     * @return true si el proceso terminó, false si aún le quedan instrucciones
     */
    public boolean ejecutarQuantum(int quantum) {
        pcb.setEstado(EstadoProceso.EJECUCION);

        int ejecutadas = 0;
        while (ejecutadas < quantum && pcb.getProgramCounter() < instruccionesTotales) {
            pcb.incrementarPC();
            pcb.incrementarMAR();
            ejecutadas++;
        }

        if (pcb.getProgramCounter() >= instruccionesTotales) {
            pcb.setEstado(EstadoProceso.TERMINADO);
            return true; // Proceso finalizó
        } else {
            pcb.setEstado(EstadoProceso.LISTO);
            return false; // Aún le quedan instrucciones
        }
    }
    
    // fields (ya en tu clase; añadir estos)
    private int mlfqNivel = 0;        // nivel actual en MLFQ (0 = más alta prioridad)
    private int mlfqTiempoEspera = 0; // para aging

    // getters/setters para MLFQ
    public int getMlfqNivel() { return mlfqNivel; }
    public void setMlfqNivel(int nivel) { this.mlfqNivel = nivel; }

    public void incrementarMlfqEspera() { this.mlfqTiempoEspera++; }
    public int getMlfqTiempoEspera() { return mlfqTiempoEspera; }
    public void resetMlfqEspera() { this.mlfqTiempoEspera = 0; }

    // demote/promote helpers
    public void demoteMlfq() {
        this.mlfqNivel = this.mlfqNivel + 1;
    }

    public void promoteMlfq() {
        if (this.mlfqNivel > 0) this.mlfqNivel = this.mlfqNivel - 1;
    }


    @Override
    public String toString() {
        return pcb.toString();
    }
}
