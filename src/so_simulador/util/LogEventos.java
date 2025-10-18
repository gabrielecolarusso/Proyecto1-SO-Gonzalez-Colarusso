package so_simulador.util;

import so_simulador.modelo.Proceso;
import so_simulador.modelo.EstadoProceso;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Sistema de logging de eventos del simulador.
 * Registra todas las decisiones importantes del planificador y cambios de estado.
 */
public class LogEventos {
    private static LogEventos instancia = null;
    private PrintWriter writer;
    private boolean habilitado;
    private SimpleDateFormat formatoTiempo;
    private String archivoActual;
    
    // Buffer en memoria para GUI (máximo 1000 eventos)
    private String[] bufferEventos;
    private int indiceBuffer;
    private int totalEventos;
    private static final int MAX_BUFFER = 1000;
    
    private LogEventos() {
        this.habilitado = true;
        this.formatoTiempo = new SimpleDateFormat("HH:mm:ss.SSS");
        this.bufferEventos = new String[MAX_BUFFER];
        this.indiceBuffer = 0;
        this.totalEventos = 0;
    }
    
    public static synchronized LogEventos getInstance() {
        if (instancia == null) {
            instancia = new LogEventos();
        }
        return instancia;
    }
    
    /**
     * Inicia un nuevo archivo de log
     */
    public boolean iniciarLog(String nombreArchivo) {
        try {
            cerrarLog();
            
            if (nombreArchivo == null || nombreArchivo.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                nombreArchivo = "simulacion_" + sdf.format(new Date()) + ".log";
            }
            
            this.archivoActual = nombreArchivo;
            this.writer = new PrintWriter(new FileWriter(nombreArchivo, false));
            
            // Escribir encabezado
            escribirLinea("═".repeat(80));
            escribirLinea("LOG DE SIMULACIÓN - SISTEMA OPERATIVO");
            escribirLinea("Inicio: " + new Date());
            escribirLinea("═".repeat(80));
            escribirLinea("");
            
            return true;
        } catch (IOException e) {
            System.err.println("Error al iniciar log: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Cierra el archivo de log actual
     */
    public void cerrarLog() {
        if (writer != null) {
            escribirLinea("");
            escribirLinea("═".repeat(80));
            escribirLinea("Fin del log: " + new Date());
            escribirLinea("Total de eventos: " + totalEventos);
            escribirLinea("═".repeat(80));
            writer.close();
            writer = null;
        }
    }
    
    // ========== MÉTODOS DE LOGGING ESPECÍFICOS ==========
    
    /**
     * Registra que el planificador seleccionó un proceso
     */
    public void logSeleccion(int ciclo, String algoritmo, Proceso proceso) {
        String msg = String.format("[Ciclo %d] PLANIFICADOR (%s) → Selecciona: %s (ID:%d, PC:%d/%d)",
            ciclo, algoritmo, proceso.getPCB().getNombre(), 
            proceso.getPCB().getId(),
            proceso.getPCB().getProgramCounter(),
            proceso.getInstruccionesTotales());
        registrar(msg, TipoEvento.SELECCION);
    }
    
    /**
     * Registra cambio de estado de un proceso
     */
    public void logCambioEstado(int ciclo, Proceso proceso, EstadoProceso estadoAnterior, EstadoProceso estadoNuevo) {
        String msg = String.format("[Ciclo %d] %s: %s → %s",
            ciclo, proceso.getPCB().getNombre(), estadoAnterior, estadoNuevo);
        registrar(msg, TipoEvento.CAMBIO_ESTADO);
    }
    
    /**
     * Registra bloqueo por E/S
     */
    public void logBloqueo(int ciclo, Proceso proceso, String motivo) {
        String msg = String.format("[Ciclo %d] BLOQUEO: %s → %s (PC:%d)",
            ciclo, proceso.getPCB().getNombre(), motivo, 
            proceso.getPCB().getProgramCounter());
        registrar(msg, TipoEvento.BLOQUEO);
    }
    
    /**
     * Registra desbloqueo de un proceso
     */
    public void logDesbloqueo(int ciclo, Proceso proceso) {
        String msg = String.format("[Ciclo %d] DESBLOQUEO: %s → LISTO",
            ciclo, proceso.getPCB().getNombre());
        registrar(msg, TipoEvento.DESBLOQUEO);
    }
    
    /**
     * Registra operación de swapping
     */
    public void logSwap(int ciclo, Proceso proceso, String politica, boolean entrada) {
        String operacion = entrada ? "SWAP IN" : "SWAP OUT";
        String msg = String.format("[Ciclo %d] %s: %s (Política: %s)",
            ciclo, operacion, proceso.getPCB().getNombre(), politica);
        registrar(msg, TipoEvento.SWAP);
    }
    
    /**
     * Registra finalización de un proceso
     */
    public void logFinalizacion(int ciclo, Proceso proceso) {
        String msg = String.format("[Ciclo %d] TERMINADO: %s (Instrucciones: %d/%d)",
            ciclo, proceso.getPCB().getNombre(),
            proceso.getPCB().getProgramCounter(),
            proceso.getInstruccionesTotales());
        registrar(msg, TipoEvento.FINALIZACION);
    }
    
    /**
     * Registra admisión de proceso al sistema
     */
    public void logAdmision(int ciclo, Proceso proceso) {
        String tipo = proceso.isCPUbound() ? "CPU-bound" : "I/O-bound";
        String msg = String.format("[Ciclo %d] ADMISIÓN: %s (ID:%d, %s, %d inst, Prior:%d)",
            ciclo, proceso.getPCB().getNombre(), proceso.getPCB().getId(),
            tipo, proceso.getInstruccionesTotales(), proceso.getPrioridad());
        registrar(msg, TipoEvento.ADMISION);
    }
    
    /**
     * Registra cambio de planificador
     */
    public void logCambioPlanificador(int ciclo, String anterior, String nuevo) {
        String msg = String.format("[Ciclo %d] CAMBIO PLANIFICADOR: %s → %s",
            ciclo, anterior, nuevo);
        registrar(msg, TipoEvento.SISTEMA);
    }
    
    /**
     * Registra agotamiento de quantum en Round Robin
     */
    public void logQuantumAgotado(int ciclo, Proceso proceso, int quantum) {
        String msg = String.format("[Ciclo %d] QUANTUM AGOTADO: %s (q=%d) → Cola de listos",
            ciclo, proceso.getPCB().getNombre(), quantum);
        registrar(msg, TipoEvento.QUANTUM);
    }
    
    /**
     * Registra CPU inactiva
     */
    public void logCPUInactiva(int ciclo, String motivo) {
        String msg = String.format("[Ciclo %d] CPU INACTIVA: %s",
            ciclo, motivo);
        registrar(msg, TipoEvento.CPU_IDLE);
    }
    
    /**
     * Registra operación del planificador de largo plazo
     */
    public void logLargoPlazo(int ciclo, int procesosAdmitidos, int enEspera) {
        String msg = String.format("[Ciclo %d] PLANIF. LARGO PLAZO: %d procesos cargados, %d en espera",
            ciclo, procesosAdmitidos, enEspera);
        registrar(msg, TipoEvento.LARGO_PLAZO);
    }
    
    /**
     * Registra operación del planificador de mediano plazo
     */
    public void logMedianoPlazo(int ciclo, int procesosReanudados) {
        if (procesosReanudados > 0) {
            String msg = String.format("[Ciclo %d] PLANIF. MEDIANO PLAZO: %d procesos reanudados",
                ciclo, procesosReanudados);
            registrar(msg, TipoEvento.MEDIANO_PLAZO);
        }
    }
    
    /**
     * Registra ejecución de instrucciones
     */
    public void logEjecucion(int ciclo, Proceso proceso, int instruccionesEjecutadas) {
        String msg = String.format("[Ciclo %d] EJECUCIÓN: %s ejecutó %d instrucciones (PC:%d/%d)",
            ciclo, proceso.getPCB().getNombre(), instruccionesEjecutadas,
            proceso.getPCB().getProgramCounter(), proceso.getInstruccionesTotales());
        registrar(msg, TipoEvento.EJECUCION);
    }
    
    /**
     * Registra evento genérico del sistema
     */
    public void logSistema(String mensaje) {
        registrar(mensaje, TipoEvento.SISTEMA);
    }
    
    /**
     * Registra advertencia
     */
    public void logAdvertencia(String mensaje) {
        registrar("⚠ ADVERTENCIA: " + mensaje, TipoEvento.ADVERTENCIA);
    }
    
    /**
     * Registra error
     */
    public void logError(String mensaje) {
        registrar("✗ ERROR: " + mensaje, TipoEvento.ERROR);
    }
    
    // ========== MÉTODOS AUXILIARES ==========
    
    /**
     * Método central de registro
     */
    private void registrar(String mensaje, TipoEvento tipo) {
        if (!habilitado) return;
        
        String timestamp = formatoTiempo.format(new Date());
        String lineaCompleta = String.format("[%s] %s", timestamp, mensaje);
        
        // Escribir a archivo
        escribirLinea(lineaCompleta);
        
        // Agregar a buffer para GUI
        agregarABuffer(lineaCompleta);
        
        totalEventos++;
    }
    
    private void escribirLinea(String linea) {
        if (writer != null) {
            writer.println(linea);
            writer.flush(); // Asegurar que se escriba inmediatamente
        }
    }
    
    private void agregarABuffer(String evento) {
        bufferEventos[indiceBuffer] = evento;
        indiceBuffer = (indiceBuffer + 1) % MAX_BUFFER;
    }
    
    /**
     * Obtiene los últimos N eventos para mostrar en GUI
     */
    public String[] getUltimosEventos(int n) {
        if (n > MAX_BUFFER) n = MAX_BUFFER;
        if (n > totalEventos) n = totalEventos;
        
        String[] resultado = new String[n];
        int inicio = (indiceBuffer - n + MAX_BUFFER) % MAX_BUFFER;
        
        for (int i = 0; i < n; i++) {
            int idx = (inicio + i) % MAX_BUFFER;
            resultado[i] = bufferEventos[idx];
            if (resultado[i] == null) resultado[i] = "";
        }
        
        return resultado;
    }
    
    /**
     * Obtiene todos los eventos del buffer
     */
    public String[] getTodosLosEventos() {
        int cantidad = Math.min(totalEventos, MAX_BUFFER);
        return getUltimosEventos(cantidad);
    }
    
    /**
     * Limpia el buffer de eventos
     */
    public void limpiarBuffer() {
        for (int i = 0; i < MAX_BUFFER; i++) {
            bufferEventos[i] = null;
        }
        indiceBuffer = 0;
    }
    
    /**
     * Exporta el log completo a un archivo
     */
    public boolean exportarLog(String rutaDestino) {
        try (BufferedReader reader = new BufferedReader(new FileReader(archivoActual));
             PrintWriter writer = new PrintWriter(new FileWriter(rutaDestino))) {
            
            String linea;
            while ((linea = reader.readLine()) != null) {
                writer.println(linea);
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("Error al exportar log: " + e.getMessage());
            return false;
        }
    }
    
    // Getters y setters
    
    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }
    
    public boolean isHabilitado() {
        return habilitado;
    }
    
    public int getTotalEventos() {
        return totalEventos;
    }
    
    public String getArchivoActual() {
        return archivoActual;
    }
    
    /**
     * Tipos de eventos para categorización
     */
    private enum TipoEvento {
        SELECCION,
        CAMBIO_ESTADO,
        BLOQUEO,
        DESBLOQUEO,
        SWAP,
        FINALIZACION,
        ADMISION,
        QUANTUM,
        CPU_IDLE,
        LARGO_PLAZO,
        MEDIANO_PLAZO,
        EJECUCION,
        SISTEMA,
        ADVERTENCIA,
        ERROR
    }
}