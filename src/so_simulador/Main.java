package so_simulador;

import so_simulador.modelo.*;

public class Main {
    public static void main(String[] args) {
        // Crear procesos
        Proceso p1 = new Proceso("Proceso A", 5, true, 0, 0);
        Proceso p2 = new Proceso("Proceso B", 3, false, 2, 1);

        // PCB inicial
        System.out.println("PCB inicial P1: " + p1.getPCB());
        System.out.println("PCB inicial P2: " + p2.getPCB());

        // Cola de procesos
        ColaProcesos cola = new ColaProcesos();
        cola.encolar(p1);
        cola.encolar(p2);

        System.out.println("\nCola de procesos:");
        cola.imprimirCola();

        // CPU
        CPU cpu = new CPU();
        cpu.cargarProceso(p1);
        System.out.println("\nEjecutando en CPU: " + cpu.getProcesoActual());

        // Simular ejecución
        p1.run(); // ejecuta 5 instrucciones
        System.out.println("\nDespués de ejecución P1: " + p1.getPCB());

        cpu.liberarCPU();
        System.out.println("CPU liberada. Proceso actual: " + cpu.getProcesoActual());

        // Ejecutar segundo proceso
        cpu.cargarProceso(p2);
        p2.run(); // ejecuta 3 instrucciones
        System.out.println("\nDespués de ejecución P2: " + p2.getPCB());
    }
}
