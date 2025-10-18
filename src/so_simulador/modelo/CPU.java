package so_simulador.modelo;

/**
 * CPU corregida que respeta los estados TERMINADO y BLOQUEADO
 */
public class CPU {
    private Proceso procesoActual;
    private ContextoEjecucion contexto;

    public CPU() {
        this.procesoActual = null;
        this.contexto = ContextoEjecucion.KERNEL_MODE;
    }

    /**
     * Carga un proceso en la CPU para ejecución
     */
    public void cargarProceso(Proceso p) {
        this.procesoActual = p;
        p.getPCB().setEstado(EstadoProceso.EJECUCION);
        this.contexto = ContextoEjecucion.USER_MODE;
    }

    /**
     * Libera la CPU (desaloja el proceso actual)
     * CORRECCIÓN: Respeta estados TERMINADO y BLOQUEADO
     */
    public void liberarCPU() {
        if (procesoActual != null) {
            EstadoProceso estadoActual = procesoActual.getPCB().getEstado();
            
            // Solo cambiar a LISTO si el proceso NO terminó y NO está bloqueado
            if (estadoActual != EstadoProceso.TERMINADO && 
                estadoActual != EstadoProceso.BLOQUEADO &&
                estadoActual != EstadoProceso.SUSPENDIDO) {
                procesoActual.getPCB().setEstado(EstadoProceso.LISTO);
            }
            
            procesoActual = null;
        }
        
        this.contexto = ContextoEjecucion.KERNEL_MODE;
    }

    /**
     * Obtiene el proceso que está ejecutándose actualmente
     */
    public Proceso getProcesoActual() {
        return procesoActual;
    }
    
    /**
     * Verifica si la CPU está ocupada
     */
    public boolean estaOcupada() {
        return procesoActual != null;
    }
    
    /**
     * Obtiene el contexto de ejecución actual
     */
    public ContextoEjecucion getContexto() {
        return contexto;
    }
    
    /**
     * Cambia el contexto de ejecución
     */
    public void setContexto(ContextoEjecucion contexto) {
        this.contexto = contexto;
    }
    
    /**
     * Devuelve información del estado actual de la CPU
     */
    public String getEstado() {
        if (procesoActual == null) {
            return "CPU INACTIVA (Modo: " + contexto + ")";
        }
        return "Ejecutando: " + procesoActual.getPCB().getNombre() + 
               " (Modo: " + contexto + ", PC: " + procesoActual.getPCB().getProgramCounter() + 
               "/" + procesoActual.getInstruccionesTotales() + ")";
    }
    
    @Override
    public String toString() {
        return getEstado();
    }
}

/**
 * Enum para diferenciar modo kernel (SO) vs modo usuario (proceso)
 */
enum ContextoEjecucion {
    KERNEL_MODE,  // Sistema operativo ejecutándose (planificación, swapping, etc.)
    USER_MODE     // Proceso de usuario ejecutándose
}