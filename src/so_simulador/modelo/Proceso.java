package so_simulador.modelo;

import so_simulador.sincronizacion.GestorES;
import so_simulador.sincronizacion.Reloj;
import so_simulador.sincronizacion.Semaforo;

/**
 * Proceso híbrido: soporta simulación secuencial (ejecutarQuantum/run como antes)
 * y concurrencia real con bloqueo por semáforos y E/S.
 */
public class Proceso extends Thread {
    private PCB pcb;
    private int instruccionesTotales;
    private boolean esCPUbound;
    private int ciclosExcepcion;
    private int ciclosAtencion;
    private int prioridad;
    private int tiempoLlegada;
    private int tiempoEspera;
    
    // Control de E/S para simulación secuencial
    private int contadorES;  // ciclos restantes de E/S
    private int duracionES;  // duración total de la E/S

    // MLFQ
    private int mlfqNivel = 0;
    private int mlfqTiempoEspera = 0;

    // Concurrencia
    private Reloj reloj;
    private Semaforo recurso;

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
        this.contadorES = 0;
        this.duracionES = 2; // por defecto 2 ciclos de E/S
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

    // Control de E/S
    public void iniciarES() {
        this.contadorES = duracionES;
    }
    
    public void decrementarContadorES() {
        if (contadorES > 0) contadorES--;
    }
    
    public int getContadorES() {
        return contadorES;
    }
    
    public void setDuracionES(int duracion) {
        this.duracionES = duracion;
    }

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
     */
    @Override
    public void run() {
        pcb.setEstado(EstadoProceso.EJECUCION);

        while (pcb.getProgramCounter() < instruccionesTotales) {
            if (reloj != null) reloj.tick();

            pcb.incrementarPC();
            pcb.incrementarMAR();

            // Bloqueo por semáforo
            if (recurso != null && ciclosExcepcion > 0 && pcb.getProgramCounter() == ciclosExcepcion) {
                synchronized (System.out) {
                    System.out.println("[" + pcb.getNombre() + "] intentando acceder al recurso...");
                }

                pcb.setEstado(EstadoProceso.BLOQUEADO);
                recurso.waitSem();

                synchronized (System.out) {
                    System.out.println("[" + pcb.getNombre() + "] obtuvo el recurso ✅");
                }

                pcb.setEstado(EstadoProceso.EJECUCION);
            }

            // E/S cada ciclosAtencion instrucciones
            if (ciclosAtencion > 0 && (pcb.getProgramCounter() % ciclosAtencion) == 0 && pcb.getProgramCounter() < instruccionesTotales) {
                synchronized (System.out) {
                    System.out.println("[" + pcb.getNombre() + "] inicia operación de E/S → BLOQUEADO");
                }

                pcb.setEstado(EstadoProceso.BLOQUEADO);

                boolean ok = GestorES.getInstance().registrarBloqueado(this, 3);
                if (!ok) {
                    try { Thread.sleep(100); } catch (InterruptedException ex) {}
                } else {
                    synchronized (this) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {}
                    }
                }

                pcb.setEstado(EstadoProceso.LISTO);
                synchronized (System.out) {
                    System.out.println("[" + pcb.getNombre() + "] E/S completada → LISTO");
                }
                pcb.setEstado(EstadoProceso.EJECUCION);
            }

            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
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

    public synchronized void reanudarPorES() {
        this.notify();
    }

    public synchronized void reanudarManual() {
        this.notify();
    }

    public void suspender() {
        pcb.setEstado(EstadoProceso.SUSPENDIDO);
        this.interrupt();
    }

    /**
     * Ejecutar quantum para simulación secuencial (usado en Main.java)
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