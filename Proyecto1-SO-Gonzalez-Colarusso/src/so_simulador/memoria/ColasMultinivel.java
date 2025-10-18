package so_simulador.memoria;

import so_simulador.modelo.*;

/**
 * Gestiona las tres colas de planificación según su plazo:
 * - Largo plazo: procesos nuevos esperando admisión
 * - Mediano plazo: procesos suspendidos (swapped out)
 * - Corto plazo: procesos listos para ejecutar
 */
public class ColasMultinivel {
    private ColaProcesos colaLargoPlazo;    // NEW
    private ColaProcesos colaMedianoPlazo;  // SUSPENDED
    private ColaProcesos colaCortoPlazo;    // READY
    private ColaProcesos colaBloqueados;    // BLOCKED
    
    private GestorMemoria gestorMemoria;
    
    public ColasMultinivel(GestorMemoria gestorMemoria) {
        this.colaLargoPlazo = new ColaProcesos();
        this.colaMedianoPlazo = new ColaProcesos();
        this.colaCortoPlazo = new ColaProcesos();
        this.colaBloqueados = new ColaProcesos();
        this.gestorMemoria = gestorMemoria;
    }
    
    /**
     * Admite un proceso nuevo al sistema (va a largo plazo)
     */
    public void admitirProceso(Proceso p) {
        p.getPCB().setEstado(EstadoProceso.NUEVO);
        colaLargoPlazo.encolar(p);
    }
    
    /**
     * Planificador de largo plazo: intenta cargar procesos nuevos en memoria
     * Retorna el número de procesos cargados
     */
    public int planificarLargoPlazo(int tiempoActual) {
        int cargados = 0;
        
        // Mientras haya espacio y procesos nuevos
        while (!colaLargoPlazo.estaVacia() && gestorMemoria.hayEspacioDisponible()) {
            Proceso p = colaLargoPlazo.desencolar();
            
            if (gestorMemoria.cargarProceso(p, tiempoActual)) {
                p.getPCB().setEstado(EstadoProceso.LISTO);
                colaCortoPlazo.encolar(p);
                cargados++;
            } else {
                // No se pudo cargar, devolver a la cola
                colaLargoPlazo.encolar(p);
                break;
            }
        }
        
        return cargados;
    }
    
    /**
     * Planificador de mediano plazo: maneja swapping
     * Intenta reanudar procesos suspendidos si hay espacio
     */
    public int planificarMedianoPlazo(int tiempoActual) {
        int reanudados = 0;
        
        // Intentar reanudar procesos suspendidos
        ColaProcesos.ColaTemporal it = colaMedianoPlazo.crearIterador();
        while (it.tieneSiguiente()) {
            Proceso p = it.siguiente();
            
            if (gestorMemoria.cargarProceso(p, tiempoActual)) {
                colaMedianoPlazo.eliminarProceso(p);
                
                // Determinar estado destino según su estado anterior
                if (p.getContadorES() > 0) {
                    p.getPCB().setEstado(EstadoProceso.BLOQUEADO);
                    colaBloqueados.encolar(p);
                } else {
                    p.getPCB().setEstado(EstadoProceso.LISTO);
                    colaCortoPlazo.encolar(p);
                }
                
                reanudados++;
            } else {
                break; // No hay más espacio
            }
        }
        
        return reanudados;
    }
    
    /**
     * Suspende un proceso (lo mueve a mediano plazo y libera memoria)
     */
    public boolean suspenderProceso(Proceso p) {
        // Buscar en corto plazo
        ColaProcesos.ColaTemporal itCorto = colaCortoPlazo.crearIterador();
        boolean encontrado = false;
        while (itCorto.tieneSiguiente()) {
            if (itCorto.siguiente() == p) {
                encontrado = true;
                break;
            }
        }
        
        if (encontrado) {
            colaCortoPlazo.eliminarProceso(p);
        } else {
            // Buscar en bloqueados
            ColaProcesos.ColaTemporal itBloq = colaBloqueados.crearIterador();
            while (itBloq.tieneSiguiente()) {
                if (itBloq.siguiente() == p) {
                    encontrado = true;
                    break;
                }
            }
            
            if (encontrado) {
                colaBloqueados.eliminarProceso(p);
            }
        }
        
        if (encontrado) {
            gestorMemoria.liberarProceso(p);
            p.getPCB().setEstado(EstadoProceso.SUSPENDIDO);
            colaMedianoPlazo.encolar(p);
            return true;
        }
        
        return false;
    }
    
    /**
     * Realiza swapping forzado para liberar memoria
     */
    public Proceso realizarSwap(int tiempoActual) {
        Proceso victima = gestorMemoria.realizarSwap(tiempoActual);
        
        if (victima != null) {
            // Mover de corto plazo o bloqueados a mediano plazo
            colaCortoPlazo.eliminarProceso(victima);
            colaBloqueados.eliminarProceso(victima);
            colaMedianoPlazo.encolar(victima);
        }
        
        return victima;
    }
    
    // Getters para acceso a las colas
    
    public ColaProcesos getColaLargoPlazo() {
        return colaLargoPlazo;
    }
    
    public ColaProcesos getColaMedianoPlazo() {
        return colaMedianoPlazo;
    }
    
    public ColaProcesos getColaCortoPlazo() {
        return colaCortoPlazo;
    }
    
    public ColaProcesos getColaBloqueados() {
        return colaBloqueados;
    }
    
    /**
     * Mueve un proceso de listo a bloqueado
     */
    public void bloquearProceso(Proceso p) {
        colaCortoPlazo.eliminarProceso(p);
        p.getPCB().setEstado(EstadoProceso.BLOQUEADO);
        colaBloqueados.encolar(p);
    }
    
    /**
     * Mueve un proceso de bloqueado a listo
     */
    public void desbloquearProceso(Proceso p) {
        colaBloqueados.eliminarProceso(p);
        p.getPCB().setEstado(EstadoProceso.LISTO);
        colaCortoPlazo.encolar(p);
    }
    
    /**
     * Reporta el estado de todas las colas
     */
    public String getResumen() {
        return String.format("Colas - Nuevo: %d | Listo: %d | Bloqueado: %d | Suspendido: %d",
                contarCola(colaLargoPlazo),
                contarCola(colaCortoPlazo),
                contarCola(colaBloqueados),
                contarCola(colaMedianoPlazo));
    }
    
    private int contarCola(ColaProcesos cola) {
        int count = 0;
        ColaProcesos.ColaTemporal it = cola.crearIterador();
        while (it.tieneSiguiente()) {
            it.siguiente();
            count++;
        }
        return count;
    }
}