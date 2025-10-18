package so_simulador.memoria;

import so_simulador.modelo.*;

/**
 * Gestiona la memoria principal y el swapping de procesos.
 * Implementa políticas de suspensión cuando no hay suficiente memoria.
 */
public class GestorMemoria {
    private int memoriaTotal;      // KB totales
    private int memoriaDisponible; // KB libres
    private int memoriaPorProceso; // KB que ocupa cada proceso
    
    private EntradaMemoria[] tabla; // Tabla de procesos en memoria
    private int maxProcesos;
    private int procesosEnMemoria;
    
    private static final int MAX_ENTRADAS = 256;
    
    // Políticas de swapping
    public enum PoliticaSwap {
        LRU,           // Least Recently Used
        PRIORIDAD,     // Menor prioridad primero
        TIEMPO_ESPERA  // Más tiempo esperando primero
    }
    
    private PoliticaSwap politica;
    
    private static class EntradaMemoria {
        Proceso proceso;
        int tiempoUltimoAcceso;
        boolean enUso;
        
        EntradaMemoria() {
            this.enUso = false;
        }
    }
    
    public GestorMemoria(int memoriaTotal, int memoriaPorProceso) {
        this.memoriaTotal = memoriaTotal;
        this.memoriaPorProceso = memoriaPorProceso;
        this.maxProcesos = memoriaTotal / memoriaPorProceso;
        this.memoriaDisponible = memoriaTotal;
        this.procesosEnMemoria = 0;
        this.politica = PoliticaSwap.LRU;
        
        this.tabla = new EntradaMemoria[MAX_ENTRADAS];
        for (int i = 0; i < MAX_ENTRADAS; i++) {
            tabla[i] = new EntradaMemoria();
        }
    }
    
    /**
     * Intenta cargar un proceso en memoria.
     * Retorna true si se cargó exitosamente, false si debe ser suspendido.
     */
    public boolean cargarProceso(Proceso p, int tiempoActual) {
        // Verificar si ya está en memoria
        if (estaEnMemoria(p)) {
            actualizarAcceso(p, tiempoActual);
            return true;
        }
        
        // Verificar si hay espacio disponible
        if (memoriaDisponible >= memoriaPorProceso) {
            // Hay espacio, cargar directamente
            agregarAMemoria(p, tiempoActual);
            return true;
        }
        
        // No hay espacio, el proceso debe ser suspendido
        return false;
    }
    
    /**
     * Libera la memoria ocupada por un proceso (cuando termina o se suspende)
     */
    public void liberarProceso(Proceso p) {
        for (int i = 0; i < MAX_ENTRADAS; i++) {
            if (tabla[i].enUso && tabla[i].proceso == p) {
                tabla[i].enUso = false;
                tabla[i].proceso = null;
                memoriaDisponible += memoriaPorProceso;
                procesosEnMemoria--;
                return;
            }
        }
    }
    
    /**
     * Realiza swapping: expulsa un proceso según la política activa
     * y retorna el proceso expulsado (o null si no se pudo)
     */
    public Proceso realizarSwap(int tiempoActual) {
        if (procesosEnMemoria == 0) return null;
        
        Proceso victima = null;
        
        switch (politica) {
            case LRU:
                victima = seleccionarVictimaLRU();
                break;
            case PRIORIDAD:
                victima = seleccionarVictimaPrioridad();
                break;
            case TIEMPO_ESPERA:
                victima = seleccionarVictimaTiempoEspera();
                break;
        }
        
        if (victima != null) {
            liberarProceso(victima);
            victima.getPCB().setEstado(EstadoProceso.SUSPENDIDO);
        }
        
        return victima;
    }
    
    /**
     * Selecciona víctima usando LRU (Least Recently Used)
     */
    private Proceso seleccionarVictimaLRU() {
        int indiceMenor = -1;
        int menorTiempo = Integer.MAX_VALUE;
        
        for (int i = 0; i < MAX_ENTRADAS; i++) {
            if (tabla[i].enUso) {
                // Solo considerar procesos que NO estén en ejecución
                if (tabla[i].proceso.getPCB().getEstado() != EstadoProceso.EJECUCION) {
                    if (tabla[i].tiempoUltimoAcceso < menorTiempo) {
                        menorTiempo = tabla[i].tiempoUltimoAcceso;
                        indiceMenor = i;
                    }
                }
            }
        }
        
        return (indiceMenor >= 0) ? tabla[indiceMenor].proceso : null;
    }
    
    /**
     * Selecciona víctima por menor prioridad
     */
    private Proceso seleccionarVictimaPrioridad() {
        int indiceMenor = -1;
        int menorPrioridad = -1;
        
        for (int i = 0; i < MAX_ENTRADAS; i++) {
            if (tabla[i].enUso) {
                if (tabla[i].proceso.getPCB().getEstado() != EstadoProceso.EJECUCION) {
                    int prioridad = tabla[i].proceso.getPrioridad();
                    if (prioridad > menorPrioridad) { // Mayor número = menor prioridad
                        menorPrioridad = prioridad;
                        indiceMenor = i;
                    }
                }
            }
        }
        
        return (indiceMenor >= 0) ? tabla[indiceMenor].proceso : null;
    }
    
    /**
     * Selecciona víctima por mayor tiempo en espera
     */
    private Proceso seleccionarVictimaTiempoEspera() {
        int indiceMayor = -1;
        int mayorEspera = -1;
        
        for (int i = 0; i < MAX_ENTRADAS; i++) {
            if (tabla[i].enUso) {
                if (tabla[i].proceso.getPCB().getEstado() == EstadoProceso.LISTO) {
                    int espera = tabla[i].proceso.getTiempoEspera();
                    if (espera > mayorEspera) {
                        mayorEspera = espera;
                        indiceMayor = i;
                    }
                }
            }
        }
        
        return (indiceMayor >= 0) ? tabla[indiceMayor].proceso : null;
    }
    
    // Métodos auxiliares
    
    private void agregarAMemoria(Proceso p, int tiempo) {
        for (int i = 0; i < MAX_ENTRADAS; i++) {
            if (!tabla[i].enUso) {
                tabla[i].proceso = p;
                tabla[i].tiempoUltimoAcceso = tiempo;
                tabla[i].enUso = true;
                memoriaDisponible -= memoriaPorProceso;
                procesosEnMemoria++;
                return;
            }
        }
    }
    
    private boolean estaEnMemoria(Proceso p) {
        for (int i = 0; i < MAX_ENTRADAS; i++) {
            if (tabla[i].enUso && tabla[i].proceso == p) {
                return true;
            }
        }
        return false;
    }
    
    private void actualizarAcceso(Proceso p, int tiempo) {
        for (int i = 0; i < MAX_ENTRADAS; i++) {
            if (tabla[i].enUso && tabla[i].proceso == p) {
                tabla[i].tiempoUltimoAcceso = tiempo;
                return;
            }
        }
    }
    
    // Getters y setters
    
    public int getMemoriaDisponible() { 
        return memoriaDisponible; 
    }
    
    public int getProcesosEnMemoria() { 
        return procesosEnMemoria; 
    }
    
    public int getMaxProcesos() { 
        return maxProcesos; 
    }
    
    public boolean hayEspacioDisponible() {
        return memoriaDisponible >= memoriaPorProceso;
    }
    
    public void setPolitica(PoliticaSwap politica) {
        this.politica = politica;
    }
    
    public PoliticaSwap getPolitica() {
        return politica;
    }
    
    /**
     * Devuelve estadísticas de uso de memoria
     */
    public String getEstadisticas() {
        double porcentajeUso = ((memoriaTotal - memoriaDisponible) * 100.0) / memoriaTotal;
        return String.format("Memoria: %d/%d KB (%.1f%% usado) | Procesos: %d/%d",
                memoriaTotal - memoriaDisponible, memoriaTotal, porcentajeUso,
                procesosEnMemoria, maxProcesos);
    }
}