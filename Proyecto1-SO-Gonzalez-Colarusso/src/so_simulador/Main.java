package so_simulador;

import so_simulador.modelo.*;
import so_simulador.planificador.*;
import so_simulador.util.Metricas;
import so_simulador.util.LogEventos;
import so_simulador.config.*;
import so_simulador.memoria.*;
import java.util.Scanner;

/**
 * Main corregido con todas las funcionalidades integradas
 */
public class Main {
    
    private static Scanner scanner = new Scanner(System.in);
    private static LogEventos log = LogEventos.getInstance();
    
    public static void main(String[] args) {
        imprimirBanner();
        
        // ========== FASE 1: MENÚ PRINCIPAL ==========
        while (true) {
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║           MENÚ PRINCIPAL               ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.println("1. Ejecutar simulación");
            System.out.println("2. Configurar sistema");
            System.out.println("3. Ver configuración actual");
            System.out.println("4. Salir");
            System.out.print("\nSeleccione opción: ");
            
            int opcion = leerEntero(1, 4);
            
            switch (opcion) {
                case 1:
                    ejecutarSimulacion();
                    break;
                case 2:
                    configurarSistema();
                    break;
                case 3:
                    verConfiguracion();
                    break;
                case 4:
                    System.out.println("\n✓ ¡Hasta pronto!");
                    scanner.close();
                    return;
            }
        }
    }
    
    private static void ejecutarSimulacion() {
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("    CONFIGURACIÓN DE SIMULACIÓN");
        System.out.println("═══════════════════════════════════════\n");
        
        // Iniciar log
        log.iniciarLog(null);
        log.logSistema("=== INICIO DE SIMULACIÓN ===");
        
        // ========== CARGAR CONFIGURACIÓN ==========
        ConfiguracionSimulacion config = cargarConfiguracion();
        System.out.println("✓ Configuración cargada:");
        System.out.println("  - Duración de ciclo: " + config.getCicloDuracion() + "ms");
        System.out.println("  - Memoria total: " + config.getMemoriaTotal() + "KB");
        System.out.println("  - Memoria por proceso: " + config.getMemoriaPorProceso() + "KB");
        System.out.println("  - Procesos configurados: " + config.contarProcesos() + "\n");
        
        log.logSistema("Configuración cargada - Ciclo: " + config.getCicloDuracion() + 
                      "ms, Memoria: " + config.getMemoriaTotal() + "KB");
        
        // ========== SELECCIONAR ALGORITMO ==========
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   SELECCIONE ALGORITMO DE PLANIFICACIÓN║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("1. FCFS (First Come, First Served)");
        System.out.println("2. SJF (Shortest Job First)");
        System.out.println("3. Round Robin");
        System.out.println("4. Prioridades");
        System.out.println("5. HRRN (Highest Response Ratio Next)");
        System.out.println("6. MLFQ (Multilevel Feedback Queue)");
        System.out.print("\nAlgoritmo [1-6]: ");
        
        int algoritmo = leerEntero(1, 6);
        int quantum = 2;
        
        if (algoritmo == 3) {
            System.out.print("Quantum para Round Robin [1-10]: ");
            quantum = leerEntero(1, 10);
        }
        
        // ========== INICIALIZAR SISTEMA ==========
        GestorMemoria gestorMemoria = new GestorMemoria(
            config.getMemoriaTotal(), 
            config.getMemoriaPorProceso()
        );
        ColasMultinivel colas = new ColasMultinivel(gestorMemoria);
        
        System.out.println("\n✓ Sistema de memoria inicializado:");
        System.out.println("  " + gestorMemoria.getEstadisticas());
        System.out.println("  Capacidad máxima: " + gestorMemoria.getMaxProcesos() + " procesos en RAM\n");
        
        log.logSistema("Memoria inicializada: " + gestorMemoria.getMaxProcesos() + " procesos max");
        
        // ========== CREAR PROCESOS ==========
        System.out.println("--- Admitiendo procesos al sistema ---");
        int procesosAdmitidos = 0;
        
        for (ProcesoConfig pc : config.getProcesos()) {
            if (pc != null) {
                Proceso p = new Proceso(
                    pc.getNombre(),
                    pc.getInstrucciones(),
                    pc.isEsCPUbound(),
                    pc.getCiclosExcepcion(),
                    pc.getCiclosAtencion(),
                    pc.getPrioridad()
                );
                
                colas.admitirProceso(p);
                log.logAdmision(0, p);
                procesosAdmitidos++;
                System.out.println("✓ " + p.getPCB().getNombre() + 
                                 " (" + (p.isCPUbound() ? "CPU-bound" : "I/O-bound") + 
                                 ", " + p.getInstruccionesTotales() + " inst.)");
            }
        }
        
        System.out.println("\nTotal admitidos: " + procesosAdmitidos);
        System.out.println(colas.getResumen() + "\n");
        
        // ========== CONFIGURAR PLANIFICADOR ==========
        GestorPlanificadores gestorPlanificadores = new GestorPlanificadores();
        gestorPlanificadores.registrar(new FCFS());
        gestorPlanificadores.registrar(new SJF());
        gestorPlanificadores.registrar(new RoundRobin(quantum));
        gestorPlanificadores.registrar(new Prioridades());
        gestorPlanificadores.registrar(new HRRN());
        
        int[] quantums = {1, 2, 4};
        MLFQ mlfq = new MLFQ(3, quantums, 5);
        gestorPlanificadores.registrar(mlfq);
        
        gestorPlanificadores.seleccionar(algoritmo);
        Planificador planificador = gestorPlanificadores.getActivo();
        
        System.out.println("\n✓ Planificador activo: " + planificador.getNombre());
        log.logSistema("Planificador seleccionado: " + planificador.getNombre());
        
        // ========== CONFIRMAR INICIO ==========
        System.out.print("\n¿Iniciar simulación? (S/N): ");
        String confirmar = scanner.nextLine().trim().toLowerCase();
        if (!confirmar.equals("s") && !confirmar.equals("si")) {
            System.out.println("✗ Simulación cancelada");
            log.logSistema("Simulación cancelada por usuario");
            log.cerrarLog();
            return;
        }
        
        // ========== INICIAR SIMULACIÓN ==========
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("      INICIANDO SIMULACIÓN");
        System.out.println("═══════════════════════════════════════\n");
        
        Metricas metricas = new Metricas();
        CPU cpu = new CPU();
        int ciclo = 0;
        ColaProcesos colaTerminados = new ColaProcesos();
        
        // Planificación de largo plazo inicial
        int cargados = colas.planificarLargoPlazo(ciclo);
        System.out.println("[Ciclo " + ciclo + "] Planificador de largo plazo: " + 
                         cargados + " procesos cargados en memoria");
        log.logLargoPlazo(ciclo, cargados, colas.getColaLargoPlazo().estaVacia() ? 0 : 1);
        
        // Registrar llegada de procesos cargados
        ColaProcesos.ColaTemporal itInicial = colas.getColaCortoPlazo().crearIterador();
        while (itInicial.tieneSiguiente()) {
            Proceso p = itInicial.siguiente();
            metricas.registrarLlegada(p, ciclo);
        }
        
        // BUCLE PRINCIPAL
        if (planificador instanceof MLFQ) {
            ejecutarConMLFQ((MLFQ) planificador, colas, metricas, ciclo, 
                          gestorMemoria, colaTerminados);
        } else {
            ejecutarConAlgoritmoGeneral(planificador, colas, cpu, metricas, ciclo, 
                                      colaTerminados, gestorMemoria, config);
        }
        
        // ========== RESULTADOS FINALES ==========
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("      SIMULACIÓN FINALIZADA");
        System.out.println("═══════════════════════════════════════\n");
        
        System.out.println("--- Procesos Terminados ---");
        ColaProcesos.ColaTemporal itFinal = colaTerminados.crearIterador();
        int terminadosCount = 0;
        while (itFinal.tieneSiguiente()) {
            Proceso p = itFinal.siguiente();
            System.out.println(p.getPCB());
            terminadosCount++;
        }
        
        if (terminadosCount == 0) {
            System.out.println("(ninguno registrado - verificar logs)");
        }
        
        System.out.println("\n" + colas.getResumen());
        System.out.println(gestorMemoria.getEstadisticas());
        
        metricas.imprimirReporte();
        
        log.logSistema("=== FIN DE SIMULACIÓN ===");
        log.cerrarLog();
        
        System.out.println("\n✓ Simulación completada exitosamente");
        System.out.println("✓ Log guardado en: " + log.getArchivoActual());
        esperarEnter();
    }
    
    private static void ejecutarConMLFQ(MLFQ mlfq, ColasMultinivel colas, 
                                       Metricas metricas, int ciclo, 
                                       GestorMemoria gestorMemoria,
                                       ColaProcesos colaTerminados) {
        
        // Mover procesos de corto plazo a MLFQ
        while (!colas.getColaCortoPlazo().estaVacia()) {
            Proceso p = colas.getColaCortoPlazo().desencolar();
            p.setMlfqNivel(0);
            mlfq.encolarProceso(p);
        }
        
        ColaProcesos colaBloqueadosLocal = new ColaProcesos();
        
        while (true) {
            // Planificación de largo y mediano plazo
            int nuevos = colas.planificarLargoPlazo(ciclo);
            int reanudados = colas.planificarMedianoPlazo(ciclo);
            
            if (nuevos > 0) {
                log.logLargoPlazo(ciclo, nuevos, 0);
            }
            if (reanudados > 0) {
                log.logMedianoPlazo(ciclo, reanudados);
            }
            
            // Mover nuevos procesos de corto plazo a MLFQ
            while (!colas.getColaCortoPlazo().estaVacia()) {
                Proceso p = colas.getColaCortoPlazo().desencolar();
                p.setMlfqNivel(0);
                mlfq.encolarProceso(p);
            }
            
            // Gestión de swapping
            if (!gestorMemoria.hayEspacioDisponible() && !colas.getColaLargoPlazo().estaVacia()) {
                Proceso victima = colas.realizarSwap(ciclo);
                if (victima != null) {
                    System.out.println("[Ciclo " + ciclo + "] SWAP: " + 
                                     victima.getPCB().getNombre() + " → SUSPENDIDO");
                    log.logSwap(ciclo, victima, gestorMemoria.getPolitica().toString(), false);
                }
            }
            
            // Decrementar E/S de bloqueados
            ColaProcesos.ColaTemporal itBloq = colaBloqueadosLocal.crearIterador();
            while (itBloq.tieneSiguiente()) {
                Proceso bloq = itBloq.siguiente();
                bloq.decrementarContadorES();
                
                if (bloq.getContadorES() <= 0) {
                    colaBloqueadosLocal.eliminarProceso(bloq);
                    bloq.getPCB().setEstado(EstadoProceso.LISTO);
                    mlfq.encolarProceso(bloq);
                    System.out.println("[Ciclo " + ciclo + "] " + bloq.getPCB().getNombre() + 
                                     " → DESBLOQUEADO");
                    log.logDesbloqueo(ciclo, bloq);
                }
            }
            
            mlfq.aging();
            Proceso siguiente = mlfq.seleccionarProceso(null);
            
            if (siguiente == null) {
                if (!colaBloqueadosLocal.estaVacia()) {
                    ciclo++;
                    log.logCPUInactiva(ciclo, "Procesos bloqueados esperando E/S");
                    continue;
                }
                break;
            }
            
            gestorMemoria.cargarProceso(siguiente, ciclo);
            metricas.registrarInicio(siguiente, ciclo);
            
            System.out.println("[Ciclo " + ciclo + "] CPU → " + siguiente.getPCB().getNombre() + 
                             " (Nivel " + siguiente.getMlfqNivel() + ")");
            log.logSeleccion(ciclo, "MLFQ-Nivel" + siguiente.getMlfqNivel(), siguiente);
            
            int q = mlfq.getQuantumParaNivel(siguiente.getMlfqNivel());
            boolean terminado = false;
            boolean bloqueado = false;
            int ejecutadas = 0;
            
            for (int i = 0; i < q && !terminado && !bloqueado; i++) {
                siguiente.getPCB().incrementarPC();
                siguiente.getPCB().incrementarMAR();
                ejecutadas++;
                ciclo++;
                
                // Verificar E/S
                if (!siguiente.isCPUbound() && siguiente.getCiclosAtencion() > 0) {
                    if (siguiente.getPCB().getProgramCounter() % siguiente.getCiclosAtencion() == 0 
                        && siguiente.getPCB().getProgramCounter() < siguiente.getInstruccionesTotales()) {
                        bloqueado = true;
                        siguiente.iniciarES();
                        siguiente.getPCB().setEstado(EstadoProceso.BLOQUEADO);
                        colaBloqueadosLocal.encolar(siguiente);
                        System.out.println("[Ciclo " + ciclo + "] " + siguiente.getPCB().getNombre() + 
                                         " → BLOQUEADO (E/S)");
                        log.logBloqueo(ciclo, siguiente, "Solicitud de E/S");
                    }
                }
                
                if (siguiente.getPCB().getProgramCounter() >= siguiente.getInstruccionesTotales()) {
                    terminado = true;
                }
            }
            
            metricas.registrarEjecucion(siguiente, ejecutadas, ciclo);
            log.logEjecucion(ciclo, siguiente, ejecutadas);
            
            if (terminado) {
                siguiente.getPCB().setEstado(EstadoProceso.TERMINADO);
                metricas.registrarFinalizacion(siguiente, ciclo);
                gestorMemoria.liberarProceso(siguiente);
                colaTerminados.encolar(siguiente);
                System.out.println("[Ciclo " + ciclo + "] " + siguiente.getPCB().getNombre() + 
                                 " → TERMINADO");
                log.logFinalizacion(ciclo, siguiente);
            } else if (!bloqueado) {
                if (siguiente.getMlfqNivel() < mlfq.getNivelesCount() - 1) {
                    siguiente.demoteMlfq();
                }
                mlfq.encolarProceso(siguiente);
            }
        }
    }
    
    private static void ejecutarConAlgoritmoGeneral(Planificador planificador, 
                                                   ColasMultinivel colas, CPU cpu,
                                                   Metricas metricas, int ciclo,
                                                   ColaProcesos colaTerminados,
                                                   GestorMemoria gestorMemoria,
                                                   ConfiguracionSimulacion config) {
        
        while (!colas.getColaCortoPlazo().estaVacia() || 
               !colas.getColaBloqueados().estaVacia() ||
               !colas.getColaLargoPlazo().estaVacia() ||
               !colas.getColaMedianoPlazo().estaVacia()) {
            
            // Planificación de largo y mediano plazo
            int nuevos = colas.planificarLargoPlazo(ciclo);
            int reanudados = colas.planificarMedianoPlazo(ciclo);
            
            if (nuevos > 0) {
                System.out.println("[Ciclo " + ciclo + "] Largo plazo: " + nuevos + " procesos cargados");
                log.logLargoPlazo(ciclo, nuevos, 0);
            }
            if (reanudados > 0) {
                System.out.println("[Ciclo " + ciclo + "] Mediano plazo: " + reanudados + " procesos reanudados");
                log.logMedianoPlazo(ciclo, reanudados);
            }
            
            // Gestión de swapping
            if (!gestorMemoria.hayEspacioDisponible() && !colas.getColaLargoPlazo().estaVacia()) {
                Proceso victima = colas.realizarSwap(ciclo);
                if (victima != null) {
                    System.out.println("[Ciclo " + ciclo + "] SWAP: " + victima.getPCB().getNombre() + 
                                     " → SUSPENDIDO (política: " + gestorMemoria.getPolitica() + ")");
                    log.logSwap(ciclo, victima, gestorMemoria.getPolitica().toString(), false);
                }
            }
            
            // Decrementar E/S de bloqueados
            ColaProcesos.ColaTemporal itBloq = colas.getColaBloqueados().crearIterador();
            while (itBloq.tieneSiguiente()) {
                Proceso bloq = itBloq.siguiente();
                bloq.decrementarContadorES();
                
                if (bloq.getContadorES() <= 0) {
                    colas.desbloquearProceso(bloq);
                    System.out.println("[Ciclo " + ciclo + "] " + bloq.getPCB().getNombre() + 
                                     " → DESBLOQUEADO");
                    log.logDesbloqueo(ciclo, bloq);
                }
            }
            
            // Si no hay procesos listos, esperar
            if (colas.getColaCortoPlazo().estaVacia()) {
                if (!colas.getColaBloqueados().estaVacia() || 
                    !colas.getColaLargoPlazo().estaVacia() ||
                    !colas.getColaMedianoPlazo().estaVacia()) {
                    ciclo++;
                    System.out.println("[Ciclo " + ciclo + "] CPU inactiva (esperando procesos)");
                    log.logCPUInactiva(ciclo, "Cola de listos vacía");
                    continue;
                } else {
                    break;
                }
            }
            
            // Incrementar espera para HRRN
            if (planificador instanceof HRRN) {
                ColaProcesos.ColaTemporal aux = colas.getColaCortoPlazo().crearIterador();
                while (aux.tieneSiguiente()) {
                    aux.siguiente().incrementarEspera();
                }
            }
            
            // Seleccionar siguiente proceso
            Proceso siguiente = planificador.seleccionarProceso(colas.getColaCortoPlazo());
            if (siguiente == null) break;
            
            gestorMemoria.cargarProceso(siguiente, ciclo);
            metricas.registrarInicio(siguiente, ciclo);
            cpu.cargarProceso(siguiente);
            
            System.out.println("[Ciclo " + ciclo + "] CPU → " + siguiente.getPCB().getNombre());
            log.logSeleccion(ciclo, planificador.getNombre(), siguiente);
            
            // Ejecutar según política
            if (planificador instanceof RoundRobin) {
                ciclo = ejecutarRoundRobin((RoundRobin) planificador, siguiente, colas, 
                                         metricas, ciclo, colaTerminados, gestorMemoria);
            } else {
                ciclo = ejecutarNoPreemptive(siguiente, colas, metricas, ciclo, 
                                           colaTerminados, gestorMemoria);
            }
            
            cpu.liberarCPU();
        }
    }
    
    private static int ejecutarRoundRobin(RoundRobin rr, Proceso proceso, 
                                         ColasMultinivel colas, Metricas metricas, 
                                         int ciclo, ColaProcesos colaTerminados,
                                         GestorMemoria gestorMemoria) {
        int quantum = rr.getQuantum();
        boolean terminado = false;
        boolean bloqueado = false;
        int ejecutadas = 0;
        
        for (int i = 0; i < quantum && !terminado && !bloqueado; i++) {
            proceso.getPCB().incrementarPC();
            proceso.getPCB().incrementarMAR();
            ejecutadas++;
            ciclo++;
            
            if (!proceso.isCPUbound() && proceso.getCiclosAtencion() > 0) {
                if (proceso.getPCB().getProgramCounter() % proceso.getCiclosAtencion() == 0 
                    && proceso.getPCB().getProgramCounter() < proceso.getInstruccionesTotales()) {
                    bloqueado = true;
                    proceso.iniciarES();
                    colas.bloquearProceso(proceso);
                    System.out.println("[Ciclo " + ciclo + "] " + proceso.getPCB().getNombre() + 
                                     " → BLOQUEADO (E/S)");
                    log.logBloqueo(ciclo, proceso, "Solicitud de E/S");
                }
            }
            
            if (proceso.getPCB().getProgramCounter() >= proceso.getInstruccionesTotales()) {
                terminado = true;
            }
        }
        
        metricas.registrarEjecucion(proceso, ejecutadas, ciclo);
        log.logEjecucion(ciclo, proceso, ejecutadas);
        
        if (terminado) {
            proceso.getPCB().setEstado(EstadoProceso.TERMINADO);
            metricas.registrarFinalizacion(proceso, ciclo);
            gestorMemoria.liberarProceso(proceso);
            colaTerminados.encolar(proceso);
            System.out.println("[Ciclo " + ciclo + "] " + proceso.getPCB().getNombre() + 
                             " → TERMINADO");
            log.logFinalizacion(ciclo, proceso);
        } else if (!bloqueado) {
            proceso.getPCB().setEstado(EstadoProceso.LISTO);
            colas.getColaCortoPlazo().encolar(proceso);
            System.out.println("[Ciclo " + ciclo + "] Quantum agotado → cola");
            log.logQuantumAgotado(ciclo, proceso, quantum);
        }
        
        return ciclo;
    }
    
    private static int ejecutarNoPreemptive(Proceso proceso, ColasMultinivel colas, 
                                           Metricas metricas, int ciclo, 
                                           ColaProcesos colaTerminados,
                                           GestorMemoria gestorMemoria) {
        boolean terminado = false;
        int pcInicial = proceso.getPCB().getProgramCounter();
        
        while (!terminado && proceso.getPCB().getProgramCounter() < proceso.getInstruccionesTotales()) {
            proceso.getPCB().incrementarPC();
            proceso.getPCB().incrementarMAR();
            ciclo++;
            
            if (!proceso.isCPUbound() && proceso.getCiclosAtencion() > 0) {
                if (proceso.getPCB().getProgramCounter() % proceso.getCiclosAtencion() == 0 
                    && proceso.getPCB().getProgramCounter() < proceso.getInstruccionesTotales()) {
                    
                    int ejecutadas = proceso.getPCB().getProgramCounter() - pcInicial;
                    metricas.registrarEjecucion(proceso, ejecutadas, ciclo);
                    log.logEjecucion(ciclo, proceso, ejecutadas);
                    
                    proceso.iniciarES();
                    colas.bloquearProceso(proceso);
                    System.out.println("[Ciclo " + ciclo + "] " + proceso.getPCB().getNombre() + 
                                     " → BLOQUEADO (E/S)");
                    log.logBloqueo(ciclo, proceso, "Solicitud de E/S");
                    break;
                }
            }
            
            if (proceso.getPCB().getProgramCounter() >= proceso.getInstruccionesTotales()) {
                terminado = true;
            }
        }
        
        if (terminado) {
            int ejecutadas = proceso.getPCB().getProgramCounter() - pcInicial;
            metricas.registrarEjecucion(proceso, ejecutadas, ciclo);
            log.logEjecucion(ciclo, proceso, ejecutadas);
            
            proceso.getPCB().setEstado(EstadoProceso.TERMINADO);
            metricas.registrarFinalizacion(proceso, ciclo);
            gestorMemoria.liberarProceso(proceso);
            colaTerminados.encolar(proceso);
            System.out.println("[Ciclo " + ciclo + "] " + proceso.getPCB().getNombre() + 
                             " → TERMINADO");
            log.logFinalizacion(ciclo, proceso);
        }
        
        return ciclo;
    }
    
    // ==================== MÉTODOS AUXILIARES (sin cambios) ====================
    
    private static void configurarSistema() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║       CONFIGURACIÓN DEL SISTEMA        ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        ConfiguracionSimulacion config = cargarConfiguracion();
        
        System.out.println("1. Modificar duración de ciclo (actual: " + config.getCicloDuracion() + "ms)");
        System.out.println("2. Modificar memoria total (actual: " + config.getMemoriaTotal() + "KB)");
        System.out.println("3. Modificar memoria por proceso (actual: " + config.getMemoriaPorProceso() + "KB)");
        System.out.println("4. Agregar proceso");
        System.out.println("5. Guardar y volver");
        System.out.print("\nOpción: ");
        
        int opcion = leerEntero(1, 5);
        
        switch (opcion) {
            case 1:
                System.out.print("Nueva duración de ciclo [100-5000ms]: ");
                config.setCicloDuracion(leerEntero(100, 5000));
                break;
            case 2:
                System.out.print("Nueva memoria total [512-8192KB]: ");
                config.setMemoriaTotal(leerEntero(512, 8192));
                break;
            case 3:
                System.out.print("Nueva memoria por proceso [64-1024KB]: ");
                config.setMemoriaPorProceso(leerEntero(64, 1024));
                break;
            case 4:
                agregarProceso(config);
                break;
            case 5:
                JSONHandler.guardar(config, "simulacion_config.json");
                System.out.println("✓ Configuración guardada");
                return;
        }
        
        JSONHandler.guardar(config, "simulacion_config.json");
        System.out.println("✓ Cambios guardados");
        esperarEnter();
        configurarSistema();
    }
    
    private static void agregarProceso(ConfiguracionSimulacion config) {
        System.out.println("\n--- Nuevo Proceso ---");
        
        scanner.nextLine();
        System.out.print("Nombre: ");
        String nombre = scanner.nextLine().trim();
        if (nombre.isEmpty()) nombre = "Proceso " + (config.contarProcesos() + 1);
        
        System.out.print("Instrucciones [1-100]: ");
        int instrucciones = leerEntero(1, 100);
        
        System.out.print("¿Es CPU-bound? (S/N): ");
        boolean esCPU = scanner.nextLine().trim().toLowerCase().startsWith("s");
        
        int ciclosExc = 0;
        int ciclosAten = 0;
        
        if (!esCPU) {
            System.out.print("Ciclos para generar E/S [2-20]: ");
            ciclosAten = leerEntero(2, 20);
        }
        
        System.out.print("Prioridad [1-10, menor=mayor prioridad]: ");
        int prioridad = leerEntero(1, 10);
        
        System.out.print("Memoria requerida [64-512KB]: ");
        int memoria = leerEntero(64, 512);
        
        ProcesoConfig pc = new ProcesoConfig(nombre, instrucciones, esCPU, 
                                            ciclosExc, ciclosAten, prioridad, memoria);
        config.agregarProceso(pc);
        
        System.out.println("✓ Proceso agregado: " + nombre);
    }
    
    private static void verConfiguracion() {
        ConfiguracionSimulacion config = cargarConfiguracion();
        
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║      CONFIGURACIÓN ACTUAL              ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        System.out.println("Sistema:");
        System.out.println("  - Duración de ciclo: " + config.getCicloDuracion() + "ms");
        System.out.println("  - Memoria total: " + config.getMemoriaTotal() + "KB");
        System.out.println("  - Memoria por proceso: " + config.getMemoriaPorProceso() + "KB");
        System.out.println("  - Algoritmo inicial: " + config.getAlgoritmoInicial());
        
        System.out.println("\nProcesos configurados: " + config.contarProcesos());
        System.out.println(String.format("%-15s %-8s %-10s %-10s %-8s %-8s", 
            "Nombre", "Inst", "Tipo", "CiclosE/S", "Prior", "Mem(KB)"));
        System.out.println("─".repeat(70));
        
        for (ProcesoConfig p : config.getProcesos()) {
            if (p != null) {
                System.out.println(String.format("%-15s %-8d %-10s %-10d %-8d %-8d",
                    p.getNombre(),
                    p.getInstrucciones(),
                    p.isEsCPUbound() ? "CPU-bound" : "I/O-bound",
                    p.getCiclosAtencion(),
                    p.getPrioridad(),
                    p.getMemoriaRequerida()));
            }
        }
        
        esperarEnter();
    }
    
    private static ConfiguracionSimulacion cargarConfiguracion() {
        String archivoConfig = "simulacion_config.json";
        ConfiguracionSimulacion config = JSONHandler.cargar(archivoConfig);
        
        if (config.contarProcesos() == 0) {
            config = crearConfiguracionPorDefecto();
            JSONHandler.guardar(config, archivoConfig);
        }
        
        return config;
    }
    
    private static ConfiguracionSimulacion crearConfiguracionPorDefecto() {
        ConfiguracionSimulacion config = new ConfiguracionSimulacion();
        config.setCicloDuracion(1000);
        config.setMemoriaTotal(1024);
        config.setMemoriaPorProceso(128);
        config.setAlgoritmoInicial("FCFS");
        
        config.agregarProceso(new ProcesoConfig("Proceso A", 10, false, 0, 3, 3, 128));
        config.agregarProceso(new ProcesoConfig("Proceso B", 7, true, 0, 0, 1, 128));
        config.agregarProceso(new ProcesoConfig("Proceso C", 8, false, 0, 4, 2, 128));
        config.agregarProceso(new ProcesoConfig("Proceso D", 5, true, 0, 0, 4, 128));
        config.agregarProceso(new ProcesoConfig("Proceso E", 12, false, 0, 3, 5, 128));
        config.agregarProceso(new ProcesoConfig("Proceso F", 9, true, 0, 0, 2, 128));
        
        return config;
    }
    
    private static void imprimirBanner() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   SIMULADOR DE SISTEMAS OPERATIVOS     ║");
        System.out.println("║          Versión Mejorada 2.0          ║");
        System.out.println("║    Con Selector de Planificadores      ║");
        System.out.println("╚════════════════════════════════════════╝");
    }
    
    private static int leerEntero(int min, int max) {
        while (true) {
            try {
                String linea = scanner.nextLine().trim();
                int valor = Integer.parseInt(linea);
                if (valor >= min && valor <= max) {
                    return valor;
                }
                System.out.print("Valor debe estar entre " + min + " y " + max + ": ");
            } catch (NumberFormatException e) {
                System.out.print("Entrada inválida. Intente de nuevo: ");
            }
        }
    }
    
    private static void esperarEnter() {
        System.out.print("\nPresione ENTER para continuar...");
        scanner.nextLine();
    }
}