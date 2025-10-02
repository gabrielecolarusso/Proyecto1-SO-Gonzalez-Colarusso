package so_simulador.modelo;

import so_simulador.sincronizacion.GestorES;
import so_simulador.sincronizacion.Reloj;
import so_simulador.sincronizacion.Semaforo;

/**
 * Proceso híbrido: soporta simulación secuencial (ejecutarQuantum/run como antes)
 * y concurrencia real con bloqueo por semáforos y E/S.
 *
 * Nota: este Proceso usa wait()/notify() internamente para bloquearse y reanudarse.
 */
public class Proceso extends Thread {
    private PCB pcb;
    private int instruccionesTotales;
    private boolean esCPUbound;
    private int ciclosExcepcion;   // si >0: en ese PC pedirá semáforo
    private int ciclosAtencion;    // si >0: cada ciclosAtencion hará E/S (bloqueo gestionado por GestorES)
    private int prioridad;
    private int tiempoLlegada;
    private int tiempoEspera;

    // MLFQ
    private int mlfqNivel = 0;
    private int mlfqTiempoEspera = 0;

    // Concurrencia
    private Reloj reloj;           // reloj global (opcional)
    private Semaforo recurso;      // recurso opcional (impresora, etc.)

    public Proceso(String nombre, int instrucciones, boolean esCPUbound,
                   int ciclosExcepcion, int ciclosAtencion, int prioridad) {
        this.pcb = new PCB(nombre);
        this.instruccionesTotales = instrucciones;
        this.esCPUbound = esCPUbound;
        this.ciclosExcepcion = ciclosExcepcion;
        this.ciclosAtencion = ciclosAtencion;
        this.prioridad = prioridad;
        this.tiempoLlegada = 0;
        this.tiempoEspera = 0;
    }

    // Getters / setters
    public PCB getPCB() { return pcb; }
    public int getInstruccionesTotales() { return instruccionesTotales; }
    public boolean isCPUbound() { return esCPUbound; }
    public int getCiclosExcepcion() { return ciclosExcepcion; }
    public int getCiclosAtencion() { return ciclosAtencion; }
    public int getPrioridad() { return prioridad; }
    public void setTiempoLlegada(int ciclo) { this.tiempoLlegada = ciclo; }
    public int getTiempoLlegada() { return tiempoLlegada; }

    // HRRN
    public void incrementarEspera() { this.tiempoEspera++; }
    public int getTiempoEspera() { return tiempoEspera; }
    public void resetEspera() { this.tiempoEspera = 0; }

    public int getServicioRestante() {
        return instruccionesTotales - pcb.getProgramCounter();
    }

    // MLFQ
    public int getMlfqNivel() { return mlfqNivel; }
    public void setMlfqNivel(int nivel) { this.mlfqNivel = nivel; }
    public void incrementarMlfqEspera() { this.mlfqTiempoEspera++; }
    public int getMlfqTiempoEspera() { return mlfqTiempoEspera; }
    public void resetMlfqEspera() { this.mlfqTiempoEspera = 0; }
    public void demoteMlfq() { this.mlfqNivel++; }
    public void promoteMlfq() { if (this.mlfqNivel > 0) this.mlfqNivel--; }

    // Concurrencia: setters
    public void setRecurso(Semaforo recurso) { this.recurso = recurso; }
    public void setReloj(Reloj reloj) { this.reloj = reloj; }

    /**
     * Ejecutado cuando se inicia el Thread (modo concurrente).
     * Soporta:
     *  - avanzar instrucciones y ticks del reloj (si está conectado)
     *  - bloqueo por semáforo (recurso.waitSem())
     *  - bloqueo por E/S: registrar en GestorES y esperar notify()
     */
    @Override
    public void run() {
        pcb.setEstado(EstadoProceso.EJECUCION);

        while (pcb.getProgramCounter() < instruccionesTotales) {
            // Reloj global (si está configurado)
            if (reloj != null) reloj.tick();

            // Ejecuta una instrucción
            pcb.incrementarPC();
            pcb.incrementarMAR();

            // --- bloqueo por semáforo si corresponde ---
            if (recurso != null && ciclosExcepcion > 0 && pcb.getProgramCounter() == ciclosExcepcion) {
                synchronized (System.out) {
                    System.out.println("[" + pcb.getNombre() + "] intentando acceder al recurso...");
                }

                pcb.setEstado(EstadoProceso.BLOQUEADO);
                recurso.waitSem(); // bloquea hasta que signal() sea llamado sobre el semáforo

                synchronized (System.out) {
                    System.out.println("[" + pcb.getNombre() + "] obtuvo el recurso ✅");
                }

                pcb.setEstado(EstadoProceso.EJECUCION);
            }

            // --- simulación de E/S cada cyclesAtencion instrucciones ---
            if (ciclosAtencion > 0 && (pcb.getProgramCounter() % ciclosAtencion) == 0 && pcb.getProgramCounter() < instruccionesTotales) {
                // registrar en GestorES y esperar notificación (reanudarPorES)
                synchronized (System.out) {
                    System.out.println("[" + pcb.getNombre() + "] inicia operación de E/S → BLOQUEADO");
                }

                pcb.setEstado(EstadoProceso.BLOQUEADO);

                // Registrar para que sea despertado tras N ticks (aquí elegimos 3 ticks como ejemplo)
                boolean ok = GestorES.getInstance().registrarBloqueado(this, 3);
                if (!ok) {
                    // si no se pudo registrar, hacemos una espera corta local (fallback)
                    try { Thread.sleep(100); } catch (InterruptedException ex) {}
                } else {
                    // Espera a ser reanudado por GestorES (reanudarPorES llama notify())
                    synchronized (this) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            // continuar si es interrumpido
                        }
                    }
                }

                // reanudado
                pcb.setEstado(EstadoProceso.LISTO);
                synchronized (System.out) {
                    System.out.println("[" + pcb.getNombre() + "] E/S completada → LISTO");
                }
                // El planificador/loop que usa este Proceso decidirá cuándo ejecutarlo de nuevo.
                // En este Thread (modo simple), seguimos y continuamos ejecutando.
                pcb.setEstado(EstadoProceso.EJECUCION);
            }

            // Simulamos pequeña latencia de ejecución para ver el intercalado
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                // Si es interrumpido por suspensión podemos manejarlo aquí
                if (pcb.getEstado() == EstadoProceso.SUSPENDIDO) {
                    synchronized (System.out) {
                        System.out.println("[" + pcb.getNombre() + "] SUSPENDIDO, esperando reanudar...");
                    }
                    synchronized (this) {
                        try { this.wait(); } catch (InterruptedException ex) {}
                    }
                    synchronized (System.out) {
                        System.out.println("[" + pcb.getNombre() + "] reanudado desde SUSPENDIDO");
                    }
                    pcb.setEstado(EstadoProceso.EJECUCION);
                }
            }
        }

        pcb.setEstado(EstadoProceso.TERMINADO);

        // Si tenía recurso, liberarlo (importante para semáforos)
        if (recurso != null) {
            recurso.signal();
            synchronized (System.out) {
                System.out.println("[" + pcb.getNombre() + "] liberó el recurso 🔓");
            }
        }

        synchronized (System.out) {
            System.out.println("[" + pcb.getNombre() + "] TERMINADO");
        }
    }

    /**
     * Notifica/ despierta el proceso tras E/S. Llamado por GestorES.
     */
    public synchronized void reanudarPorES() {
        this.notify();
    }

    /**
     * Llamar para reanudar si quedó bloqueado/suspendido por otro motivo.
     */
    public synchronized void reanudarManual() {
        this.notify();
    }

    /**
     * Suspender (cambiar estado). Para la demo, simplemente setea y
     * interrumpe el thread; la lógica de reanudación debe llamar a reanudarManual().
     */
    public void suspender() {
        pcb.setEstado(EstadoProceso.SUSPENDIDO);
        this.interrupt();
    }

    // ejecutarQuantum se mantiene sin cambios para la simulación secuencial:
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
            return true;
        } else {
            pcb.setEstado(EstadoProceso.LISTO);
            return false;
        }
    }

    @Override
    public String toString() {
        return pcb.toString();
    }
}
