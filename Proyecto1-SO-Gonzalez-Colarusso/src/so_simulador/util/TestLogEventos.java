package so_simulador.util;

import so_simulador.modelo.*;

/**
 * Test del sistema de logging
 */
public class TestLogEventos {
    public static void main(String[] args) {
        System.out.println("=== TEST DE SISTEMA DE LOGS ===\n");
        
        LogEventos log = LogEventos.getInstance();
        
        // Iniciar log
        boolean iniciado = log.iniciarLog("test_simulacion.log");
        System.out.println("✓ Log iniciado: " + (iniciado ? "SÍ" : "NO"));
        System.out.println("  Archivo: " + log.getArchivoActual() + "\n");
        
        // Crear procesos de prueba
        Proceso p1 = new Proceso("Proceso A", 10, false, 0, 3, 1);
        Proceso p2 = new Proceso("Proceso B", 7, true, 0, 0, 2);
        Proceso p3 = new Proceso("Proceso C", 12, false, 0, 4, 3);
        
        System.out.println("--- Simulando eventos ---");
        
        // Evento 1: Admisión
        log.logAdmision(0, p1);
        log.logAdmision(0, p2);
        log.logAdmision(0, p3);
        System.out.println("✓ Eventos de admisión registrados");
        
        // Evento 2: Largo plazo
        log.logLargoPlazo(1, 2, 1);
        System.out.println("✓ Planificación de largo plazo registrada");
        
        // Evento 3: Selección por planificador
        log.logSeleccion(2, "FCFS", p1);
        System.out.println("✓ Selección de proceso registrada");
        
        // Evento 4: Cambio de estado
        log.logCambioEstado(2, p1, EstadoProceso.LISTO, EstadoProceso.EJECUCION);
        System.out.println("✓ Cambio de estado registrado");
        
        // Evento 5: Ejecución
        p1.getPCB().incrementarPC();
        p1.getPCB().incrementarPC();
        p1.getPCB().incrementarPC();
        log.logEjecucion(5, p1, 3);
        System.out.println("✓ Ejecución registrada");
        
        // Evento 6: Bloqueo
        log.logBloqueo(5, p1, "Solicitud de E/S");
        log.logCambioEstado(5, p1, EstadoProceso.EJECUCION, EstadoProceso.BLOQUEADO);
        System.out.println("✓ Bloqueo registrado");
        
        // Evento 7: Selección de otro proceso
        log.logSeleccion(6, "FCFS", p2);
        log.logCambioEstado(6, p2, EstadoProceso.LISTO, EstadoProceso.EJECUCION);
        System.out.println("✓ Cambio de contexto registrado");
        
        // Evento 8: Desbloqueo
        log.logDesbloqueo(8, p1);
        log.logCambioEstado(8, p1, EstadoProceso.BLOQUEADO, EstadoProceso.LISTO);
        System.out.println("✓ Desbloqueo registrado");
        
        // Evento 9: Quantum agotado (Round Robin)
        log.logQuantumAgotado(10, p2, 2);
        System.out.println("✓ Quantum agotado registrado");
        
        // Evento 10: CPU inactiva
        log.logCPUInactiva(11, "Todos los procesos bloqueados");
        System.out.println("✓ CPU inactiva registrada");
        
        // Evento 11: Swapping
        log.logSwap(12, p3, "LRU", false); // Swap out
        System.out.println("✓ Swap out registrado");
        
        log.logSwap(15, p3, "LRU", true); // Swap in
        System.out.println("✓ Swap in registrado");
        
        // Evento 12: Finalización
        p2.getPCB().incrementarPC();
        for (int i = 0; i < 6; i++) p2.getPCB().incrementarPC();
        log.logFinalizacion(17, p2);
        log.logCambioEstado(17, p2, EstadoProceso.EJECUCION, EstadoProceso.TERMINADO);
        System.out.println("✓ Finalización registrada");
        
        // Evento 13: Cambio de planificador
        log.logCambioPlanificador(18, "FCFS", "Round Robin");
        System.out.println("✓ Cambio de planificador registrado");
        
        // Evento 14: Mediano plazo
        log.logMedianoPlazo(20, 1);
        System.out.println("✓ Planificación de mediano plazo registrada");
        
        // Eventos de sistema
        log.logSistema("Configuración cargada desde archivo");
        log.logAdvertencia("Memoria casi llena (90%)");
        log.logError("No se pudo cargar proceso por falta de memoria");
        System.out.println("✓ Eventos del sistema registrados");
        
        System.out.println("\n--- Estadísticas ---");
        System.out.println("Total de eventos: " + log.getTotalEventos());
        
        // Obtener últimos 5 eventos
        System.out.println("\n--- Últimos 5 eventos (para GUI) ---");
        String[] ultimos = log.getUltimosEventos(5);
        for (int i = 0; i < ultimos.length; i++) {
            if (ultimos[i] != null && !ultimos[i].isEmpty()) {
                System.out.println((i+1) + ". " + ultimos[i]);
            }
        }
        
        // Cerrar log
        log.cerrarLog();
        System.out.println("\n✓ Log cerrado");
        System.out.println("\n=== TEST COMPLETADO ===");
        System.out.println("Revise el archivo 'test_simulacion.log' para ver el log completo");
    }
}