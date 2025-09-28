package so_simulador.modelo;

public class Proceso implements Runnable {
    private PCB pcb;
    private int instruccionesTotales;
    private boolean esCPUbound;
    private int ciclosExcepcion;
    private int ciclosAtencion;

    public Proceso(String nombre, int instrucciones, boolean esCPUbound, int ciclosExcepcion, int ciclosAtencion) {
        this.pcb = new PCB(nombre);
        this.instruccionesTotales = instrucciones;
        this.esCPUbound = esCPUbound;
        this.ciclosExcepcion = ciclosExcepcion;
        this.ciclosAtencion = ciclosAtencion;
    }

    public PCB getPCB() { return pcb; }
    public int getInstruccionesTotales() { return instruccionesTotales; }
    public boolean isCPUbound() { return esCPUbound; }
    public int getCiclosExcepcion() { return ciclosExcepcion; }
    public int getCiclosAtencion() { return ciclosAtencion; }

    @Override
    public void run() {
        pcb.setEstado(EstadoProceso.EJECUCION);
        for (int i = 0; i < instruccionesTotales; i++) {
            pcb.incrementarPC();
            pcb.incrementarMAR();
            // Aquí luego meteremos lógica de excepciones y semáforos
        }
        pcb.setEstado(EstadoProceso.TERMINADO);
    }

    @Override
    public String toString() {
        return pcb.toString();
    }
}