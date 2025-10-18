package so_simulador.util;

import so_simulador.modelo.Proceso;


public class Metricas {
    private static final int MAX_PROC = 256;

    private Entry[] entries;
    private int total;
    private int tiempoGlobal; // tiempo lógico de la simulación (ticks)

    public Metricas() {
        entries = new Entry[MAX_PROC];
        total = 0;
        tiempoGlobal = 0;
    }

    private static class Entry {
        int id;
        String nombre;
        int llegada = -1;
        int primerInicio = -1;
        int finalizacion = -1;
        int cpuTime = 0;
        int instruccionesTotales = 0;
    }

    private int findIndexById(int id) {
        for (int i = 0; i < total; i++) {
            if (entries[i].id == id) return i;
        }
        return -1;
    }

    private void ensureEntry(Proceso p) {
        int id = p.getPCB().getId();
        if (findIndexById(id) == -1) {
            if (total < MAX_PROC) {
                Entry e = new Entry();
                e.id = id;
                e.nombre = p.getPCB().getNombre();
                e.instruccionesTotales = p.getInstruccionesTotales();
                entries[total++] = e;
            } else {
                // si se supera, no registramos (situación improbable en practicas)
            }
        }
    }

    /** Registrar llegada (por lo general tick = 0 o el tick actual) */
    public void registrarLlegada(Proceso p, int tick) {
        ensureEntry(p);
        int idx = findIndexById(p.getPCB().getId());
        if (idx >= 0) {
            entries[idx].llegada = tick;
            entries[idx].instruccionesTotales = p.getInstruccionesTotales();
            if (tick > tiempoGlobal) tiempoGlobal = tick;
        }
    }

    /** Registrar primer inicio/primer respuesta (si no se registró antes). */
    public void registrarInicio(Proceso p, int tick) {
        ensureEntry(p);
        int idx = findIndexById(p.getPCB().getId());
        if (idx >= 0) {
            if (entries[idx].primerInicio == -1) {
                entries[idx].primerInicio = tick;
            }
            if (tick > tiempoGlobal) tiempoGlobal = tick;
        }
    }

    /** Registrar ejecución de 'ejecutadas' instrucciones y el tick actual (después de ejecutar). */
    public void registrarEjecucion(Proceso p, int ejecutadas, int tick) {
        ensureEntry(p);
        int idx = findIndexById(p.getPCB().getId());
        if (idx >= 0) {
            entries[idx].cpuTime += ejecutadas;
            // si nunca tuvo primer inicio, podemos inferirlo (tick - ejecutadas)
            if (entries[idx].primerInicio == -1) {
                entries[idx].primerInicio = Math.max(entries[idx].llegada, tick - ejecutadas);
            }
            if (tick > tiempoGlobal) tiempoGlobal = tick;
        }
    }

    /** Registrar finalización con el tick actual. */
    public void registrarFinalizacion(Proceso p, int tick) {
        ensureEntry(p);
        int idx = findIndexById(p.getPCB().getId());
        if (idx >= 0) {
            entries[idx].finalizacion = tick;
            if (tick > tiempoGlobal) tiempoGlobal = tick;
        }
    }

    /** Devuelve un reporte impreso por consola con métricas por proceso y agregadas. */
    public void imprimirReporte() {
        System.out.println("\n--- METRICAS DE SIMULACIÓN ---");
        if (total == 0) {
            System.out.println("No hay procesos registrados.");
            return;
        }

        int sumWait = 0;
        int sumTurn = 0;
        int sumResp = 0;
        int sumCpu = 0;
        int finishedCount = 0;

        System.out.println("Por proceso:");
        System.out.println(String.format("%-6s %-12s %-7s %-7s %-10s %-10s", "ID", "Nombre", "Wait", "Resp", "Turnaround", "CPUtime"));
        for (int i = 0; i < total; i++) {
            Entry e = entries[i];
            int wait = 0;
            int resp = 0;
            int turn = 0;
            if (e.llegada >= 0 && e.primerInicio >= 0) {
                resp = e.primerInicio - e.llegada;
                wait = resp; // en esta simulación response == wait hasta el primer inicio
            }
            if (e.llegada >= 0 && e.finalizacion >= 0) {
                turn = e.finalizacion - e.llegada;
                finishedCount++;
            }
            sumWait += wait;
            sumResp += resp;
            sumTurn += turn;
            sumCpu += e.cpuTime;

            System.out.println(String.format("%-6d %-12s %-7d %-7d %-10d %-10d",
                    e.id, e.nombre, wait, resp, turn, e.cpuTime));
        }

        double avgWait = (double) sumWait / total;
        double avgResp = (double) sumResp / total;
        double avgTurn = (double) sumTurn / total;
        double totalTime = Math.max(1, tiempoGlobal); // evitar division por cero
        double throughput = (double) finishedCount / totalTime;
        double cpuUtil = (double) sumCpu / totalTime;

        // Jain's fairness index sobre tiempos CPU (valores positivos)
        double sumX = 0.0;
        double sumX2 = 0.0;
        for (int i = 0; i < total; i++) {
            double x = entries[i].cpuTime;
            sumX += x;
            sumX2 += x * x;
        }
        double fairness = 1.0;
        if (total > 0 && sumX2 > 0) {
            fairness = (sumX * sumX) / (total * sumX2);
        }

        System.out.println("\nAgregadas:");
        System.out.println(String.format("Tiempo simulado (ticks): %d", tiempoGlobal));
        System.out.println(String.format("Procesos finalizados: %d / %d", finishedCount, total));
        System.out.println(String.format("Throughput (proc/tick): %.4f", throughput));
        System.out.println(String.format("CPU utilization (cpuTicks/tick): %.4f", cpuUtil));
        System.out.println(String.format("Avg. wait time: %.4f", avgWait));
        System.out.println(String.format("Avg. response time: %.4f", avgResp));
        System.out.println(String.format("Avg. turnaround: %.4f", avgTurn));
        System.out.println(String.format("Fairness (Jain index): %.4f", fairness));
        System.out.println("--- FIN METRICAS ---\n");
    }

    /** Devuelve el tiempo lógico global (ticks) registrado. */
    public int getTiempoGlobal() { return tiempoGlobal; }
}

