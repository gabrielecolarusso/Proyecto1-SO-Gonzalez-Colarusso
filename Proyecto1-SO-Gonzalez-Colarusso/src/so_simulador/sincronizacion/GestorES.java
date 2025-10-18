package so_simulador.sincronizacion;

import so_simulador.modelo.Proceso;

/**
 * Gestor simple de E/S (sin usar colecciones dinámicas).
 * Mantiene una tabla con entradas (proceso + ticks restantes).
 */
public class GestorES {
    private static final int MAX = 128;
    private static GestorES instancia = null;

    private final Proceso[] procesos;
    private final int[] ticks;
    private int total;

    private GestorES() {
        procesos = new Proceso[MAX];
        ticks = new int[MAX];
        total = 0;
    }

    public static synchronized GestorES getInstance() {
        if (instancia == null) instancia = new GestorES();
        return instancia;
    }

    /**
     * Registra un proceso bloqueado por E/S para ser despertado afterTicks ticks.
     * Si la tabla está llena devuelve false.
     */
    public synchronized boolean registrarBloqueado(Proceso p, int afterTicks) {
        if (total >= MAX) return false;
        procesos[total] = p;
        ticks[total] = afterTicks;
        total++;
        return true;
    }

    /**
     * Avanza un tick: decrementa contadores y despierta los procesos cuyo contador llega a 0.
     * Se asume que cuando se despierta, el propio proceso llamará a reanudar() o será notificado.
     */
    public synchronized void tick() {
        for (int i = 0; i < total; i++) {
            ticks[i]--;
        }

        // Barrido para despertar procesos donde ticks <= 0
        int write = 0;
        for (int read = 0; read < total; read++) {
            if (ticks[read] <= 0) {
                Proceso p = procesos[read];
                if (p != null) {
                    p.reanudarPorES(); // despierta el proceso que estaba en wait()
                }
                // omitimos la entrada (no la copiamos)
            } else {
                // compactar la entrada si no despierta
                procesos[write] = procesos[read];
                ticks[write] = ticks[read];
                write++;
            }
        }
        // limpiar referencias sobrantes
        for (int k = write; k < total; k++) {
            procesos[k] = null;
            ticks[k] = 0;
        }
        total = write;
    }

    public synchronized int getTotalBloqueados() {
        return total;
    }
}
