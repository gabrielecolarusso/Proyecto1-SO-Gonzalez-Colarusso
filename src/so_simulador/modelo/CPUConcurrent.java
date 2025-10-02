package so_simulador.modelo;

/**
 * CPUConcurrent: versión usada solo para MainConcurrent,
 * que ejecuta procesos como hilos reales (Thread.start()).
 */
public class CPUConcurrent {

    /**
     * Ejecuta el proceso como hilo real si aún no fue arrancado.
     */
    public void ejecutar(Proceso p) {
        if (p.getPCB().getEstado() == EstadoProceso.TERMINADO) {
            System.out.println("CPUConcurrent: " + p.getPCB().getNombre() + " ya terminó.");
            return;
        }

        if (!p.isAlive()) {
            System.out.println("CPUConcurrent: iniciando hilo de " + p.getPCB().getNombre());
            p.start();
        } else {
            System.out.println("CPUConcurrent: " + p.getPCB().getNombre() + " ya está corriendo.");
        }
    }

    public void liberarCPU() {
        // no hace nada aquí; en concurrencia real cada hilo maneja su propio estado
    }
}
