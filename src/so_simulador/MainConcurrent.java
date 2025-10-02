package so_simulador;

import so_simulador.modelo.*;
import so_simulador.sincronizacion.*;

/**
 * MainConcurrent: prueba de concurrencia real con hilos y semáforos.
 * Aquí no usamos planificadores ni métricas, sino ejecución paralela real.
 */
public class MainConcurrent {
    public static void main(String[] args) {
        // Crear un recurso compartido (ej: impresora) con semáforo binario
        Semaforo impresora = new Semaforo(1);

        // Crear reloj global
        Reloj reloj = new Reloj();

        // Crear procesos (nombre, instrucciones, esCPUbound, cicloExcepcion, cicloES, prioridad)
        Proceso p1 = new Proceso("Proceso A", 5, true, 2, 0, 1);
        Proceso p2 = new Proceso("Proceso B", 7, true, 3, 0, 2);
        Proceso p3 = new Proceso("Proceso C", 4, true, 2, 0, 3);

        // Asignar recurso y reloj a cada proceso
        p1.setRecurso(impresora);
        p2.setRecurso(impresora);
        p3.setRecurso(impresora);

        p1.setReloj(reloj);
        p2.setReloj(reloj);
        p3.setReloj(reloj);

        // Crear CPU concurrente
        CPUConcurrent cpu = new CPUConcurrent();

        // Ejecutar procesos como hilos reales
        cpu.ejecutar(p1);
        cpu.ejecutar(p2);
        cpu.ejecutar(p3);

        // Avanzar el reloj global mientras haya procesos activos
        while (p1.isAlive() || p2.isAlive() || p3.isAlive()) {
            try {
                Thread.sleep(100); // simula ticks de reloj
                reloj.tick();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\n--- FIN DE LA SIMULACIÓN CONCURRENTE ---");
    }
}
