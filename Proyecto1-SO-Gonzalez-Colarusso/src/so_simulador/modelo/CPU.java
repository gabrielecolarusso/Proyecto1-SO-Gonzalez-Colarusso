package so_simulador.modelo;

public class CPU {
    private Proceso procesoActual;

    public void cargarProceso(Proceso p) {
        this.procesoActual = p;
        p.getPCB().setEstado(EstadoProceso.EJECUCION);
    }

    public void liberarCPU() {
        if (procesoActual != null) {
            procesoActual.getPCB().setEstado(EstadoProceso.LISTO);
            procesoActual = null;
        }
    }

    public Proceso getProcesoActual() {
        return procesoActual;
    }
}