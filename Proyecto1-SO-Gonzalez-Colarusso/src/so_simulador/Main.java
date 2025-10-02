package so_simulador;

import so_simulador.modelo.*;
import so_simulador.planificador.*;
import so_simulador.util.Metricas;

public class Main {
    public static void main(String[] args) {
        ColaProcesos colaListos = new ColaProcesos();
        ColaProcesos colaBloqueados = new ColaProcesos();

        // Crear procesos: (nombre, instrucciones, esCPUbound, ciclosExcepcion, ciclosAtencion, prioridad)
        // Proceso I/O bound que solicita E/S cada 3 instrucciones y tarda 2 ciclos en atenderse
        colaListos.encolar(new Proceso("Proceso A", 10, false, 0, 3, 3));
        colaListos.encolar(new Proceso("Proceso B", 7, true, 0, 0, 1));
        colaListos.encolar(new Proceso("Proceso C", 8, false, 0, 4, 2));
        colaListos.encolar(new Proceso("Proceso D", 5, true, 0, 0, 4));

        // Gestor de planificadores
        GestorPlanificadores gestor = new GestorPlanificadores();
        gestor.registrar(new FCFS());
        gestor.registrar(new SJF());
        gestor.registrar(new RoundRobin(2));
        gestor.registrar(new Prioridades());
        gestor.registrar(new HRRN());

        int[] quantums = new int[]{1, 2, 4};
        MLFQ mlfq = new MLFQ(3, quantums, 5);
        gestor.registrar(mlfq);

        gestor.listarAlgoritmos();
        gestor.seleccionar(1); // Cambiar índice para probar otros (1-6)

        Planificador planificador = gestor.getActivo();
        CPU cpu = new CPU();

        System.out.println("\nUsando planificador: " + planificador.getNombre());
        System.out.println("--- Simulación con métricas, E/S y bloqueos ---\n");

        Metricas metricas = new Metricas();
        int ciclo = 0;

        // Registrar llegada de todos los procesos
        ColaProcesos.ColaTemporal itInit = colaListos.crearIterador();
        while (itInit.tieneSiguiente()) {
            Proceso p = itInit.siguiente();
            metricas.registrarLlegada(p, ciclo);
        }

        // RUTA MLFQ (con manejo de E/S y bloqueos)
        if (planificador instanceof MLFQ gestorMlfq) {
            ColaProcesos colaBloqueadosMlfq = new ColaProcesos();
            
            while (!colaListos.estaVacia()) {
                Proceso p = colaListos.desencolar();
                p.setMlfqNivel(0);
                gestorMlfq.encolarProceso(p);
            }

            while (true) {
                // Decrementar contadores de E/S de procesos bloqueados
                ColaProcesos.ColaTemporal itBloq = colaBloqueadosMlfq.crearIterador();
                while (itBloq.tieneSiguiente()) {
                    Proceso bloq = itBloq.siguiente();
                    bloq.decrementarContadorES();
                    
                    if (bloq.getContadorES() <= 0) {
                        colaBloqueadosMlfq.eliminarProceso(bloq);
                        bloq.getPCB().setEstado(EstadoProceso.LISTO);
                        gestorMlfq.encolarProceso(bloq);  // Vuelve a su nivel actual
                        System.out.println("[Ciclo " + ciclo + "] " + bloq.getPCB().getNombre() + " → DESBLOQUEADO (E/S completada)");
                    }
                }

                gestorMlfq.aging();
                Proceso siguiente = gestorMlfq.seleccionarProceso(null);
                
                if (siguiente == null) {
                    if (!colaBloqueadosMlfq.estaVacia()) {
                        ciclo++;
                        System.out.println("[Ciclo " + ciclo + "] CPU en espera (todos bloqueados en MLFQ)");
                        continue;
                    }
                    break;
                }

                metricas.registrarInicio(siguiente, ciclo);
                System.out.println("[Ciclo " + ciclo + "] Ejecutando: " + siguiente.getPCB() + " (nivel=" + siguiente.getMlfqNivel() + ")");

                int q = gestorMlfq.getQuantumParaNivel(siguiente.getMlfqNivel());
                int pcAntes = siguiente.getPCB().getProgramCounter();
                
                boolean terminado = false;
                boolean bloqueado = false;
                int ejecutadas = 0;

                // Ejecutar instrucción por instrucción para detectar E/S
                for (int i = 0; i < q && !terminado && !bloqueado; i++) {
                    siguiente.getPCB().incrementarPC();
                    siguiente.getPCB().incrementarMAR();
                    ejecutadas++;
                    ciclo++;
                    
                    // Verificar bloqueo por E/S
                    if (!siguiente.isCPUbound() && siguiente.getCiclosAtencion() > 0) {
                        if (siguiente.getPCB().getProgramCounter() % siguiente.getCiclosAtencion() == 0 
                            && siguiente.getPCB().getProgramCounter() < siguiente.getInstruccionesTotales()) {
                            bloqueado = true;
                            siguiente.iniciarES();
                            siguiente.getPCB().setEstado(EstadoProceso.BLOQUEADO);
                            colaBloqueadosMlfq.encolar(siguiente);
                            System.out.println("[Ciclo " + ciclo + "] " + siguiente.getPCB().getNombre() + " → BLOQUEADO (solicita E/S)");
                        }
                    }
                    
                    if (siguiente.getPCB().getProgramCounter() >= siguiente.getInstruccionesTotales()) {
                        terminado = true;
                    }
                }

                metricas.registrarEjecucion(siguiente, ejecutadas, ciclo);

                if (terminado) {
                    metricas.registrarFinalizacion(siguiente, ciclo);
                    System.out.println("[Ciclo " + ciclo + "] Finalizado: " + siguiente.getPCB());
                } else if (bloqueado) {
                    // Ya está en colaBloqueadosMlfq, no hace falta más
                } else {
                    // Quantum agotado sin terminar ni bloquearse
                    if (siguiente.getMlfqNivel() < gestorMlfq.getNivelesCount() - 1) siguiente.demoteMlfq();
                    gestorMlfq.encolarProceso(siguiente);
                    System.out.println("[Ciclo " + ciclo + "] Quantum agotado → nivel " + siguiente.getMlfqNivel());
                }
            }
        }
        // RUTA GENERAL CON E/S Y BLOQUEOS
        else {
            ColaProcesos colaTerminados = new ColaProcesos();

            while (!colaListos.estaVacia() || !colaBloqueados.estaVacia()) {
                
                // Decrementar contadores de E/S de procesos bloqueados
                ColaProcesos.ColaTemporal itBloq = colaBloqueados.crearIterador();
                while (itBloq.tieneSiguiente()) {
                    Proceso bloq = itBloq.siguiente();
                    bloq.decrementarContadorES();
                    
                    if (bloq.getContadorES() <= 0) {
                        colaBloqueados.eliminarProceso(bloq);
                        bloq.getPCB().setEstado(EstadoProceso.LISTO);
                        colaListos.encolar(bloq);
                        System.out.println("[Ciclo " + ciclo + "] " + bloq.getPCB().getNombre() + " → DESBLOQUEADO (E/S completada)");
                    }
                }

                // Si no hay procesos listos, avanzar tiempo hasta que haya
                if (colaListos.estaVacia() && !colaBloqueados.estaVacia()) {
                    ciclo++;
                    System.out.println("[Ciclo " + ciclo + "] CPU en espera (todos bloqueados)");
                    continue;
                }

                if (colaListos.estaVacia()) break;

                // Incrementar espera para HRRN
                if (planificador instanceof HRRN) {
                    ColaProcesos.ColaTemporal aux = colaListos.crearIterador();
                    while (aux.tieneSiguiente()) aux.siguiente().incrementarEspera();
                }

                Proceso siguiente = planificador.seleccionarProceso(colaListos);
                if (siguiente == null) break;

                metricas.registrarInicio(siguiente, ciclo);
                cpu.cargarProceso(siguiente);
                System.out.println("[Ciclo " + ciclo + "] Ejecutando: " + siguiente.getPCB());

                // Round Robin con quantum
                if (planificador instanceof RoundRobin rr) {
                    int quantum = rr.getQuantum();
                    int pcAntes = siguiente.getPCB().getProgramCounter();
                    
                    boolean terminado = false;
                    boolean bloqueado = false;
                    int ejecutadasEnQuantum = 0;

                    for (int i = 0; i < quantum && !terminado && !bloqueado; i++) {
                        siguiente.getPCB().incrementarPC();
                        siguiente.getPCB().incrementarMAR();
                        ejecutadasEnQuantum++;
                        ciclo++;

                        // Verificar si debe bloquearse por E/S
                        if (!siguiente.isCPUbound() && siguiente.getCiclosAtencion() > 0) {
                            if (siguiente.getPCB().getProgramCounter() % siguiente.getCiclosAtencion() == 0 
                                && siguiente.getPCB().getProgramCounter() < siguiente.getInstruccionesTotales()) {
                                bloqueado = true;
                                siguiente.iniciarES();
                                siguiente.getPCB().setEstado(EstadoProceso.BLOQUEADO);
                                colaBloqueados.encolar(siguiente);
                                System.out.println("[Ciclo " + ciclo + "] " + siguiente.getPCB().getNombre() + " → BLOQUEADO (solicita E/S)");
                            }
                        }

                        if (siguiente.getPCB().getProgramCounter() >= siguiente.getInstruccionesTotales()) {
                            terminado = true;
                        }
                    }

                    metricas.registrarEjecucion(siguiente, ejecutadasEnQuantum, ciclo);

                    if (terminado) {
                        siguiente.getPCB().setEstado(EstadoProceso.TERMINADO);
                        metricas.registrarFinalizacion(siguiente, ciclo);
                        colaTerminados.encolar(siguiente);
                        System.out.println("[Ciclo " + ciclo + "] Finalizado: " + siguiente.getPCB());
                    } else if (!bloqueado) {
                        siguiente.getPCB().setEstado(EstadoProceso.LISTO);
                        colaListos.encolar(siguiente);
                        System.out.println("[Ciclo " + ciclo + "] Quantum terminado, regresa a cola");
                    }

                    cpu.liberarCPU();
                }
                // Algoritmos no-preemptive (FCFS, SJF, Prioridades, HRRN)
                else {
                    boolean terminado = false;
                    int pcInicial = siguiente.getPCB().getProgramCounter();

                    while (!terminado && siguiente.getPCB().getProgramCounter() < siguiente.getInstruccionesTotales()) {
                        siguiente.getPCB().incrementarPC();
                        siguiente.getPCB().incrementarMAR();
                        ciclo++;

                        // Verificar bloqueo por E/S
                        if (!siguiente.isCPUbound() && siguiente.getCiclosAtencion() > 0) {
                            if (siguiente.getPCB().getProgramCounter() % siguiente.getCiclosAtencion() == 0 
                                && siguiente.getPCB().getProgramCounter() < siguiente.getInstruccionesTotales()) {
                                
                                int ejecutadasAntes = siguiente.getPCB().getProgramCounter() - pcInicial;
                                metricas.registrarEjecucion(siguiente, ejecutadasAntes, ciclo);
                                
                                siguiente.iniciarES();
                                siguiente.getPCB().setEstado(EstadoProceso.BLOQUEADO);
                                colaBloqueados.encolar(siguiente);
                                System.out.println("[Ciclo " + ciclo + "] " + siguiente.getPCB().getNombre() + " → BLOQUEADO (solicita E/S)");
                                
                                cpu.liberarCPU();
                                break;
                            }
                        }

                        if (siguiente.getPCB().getProgramCounter() >= siguiente.getInstruccionesTotales()) {
                            terminado = true;
                        }
                    }

                    if (terminado) {
                        int totalEjecutadas = siguiente.getPCB().getProgramCounter() - pcInicial;
                        metricas.registrarEjecucion(siguiente, totalEjecutadas, ciclo);
                        siguiente.getPCB().setEstado(EstadoProceso.TERMINADO);
                        metricas.registrarFinalizacion(siguiente, ciclo);
                        colaTerminados.encolar(siguiente);
                        System.out.println("[Ciclo " + ciclo + "] Finalizado: " + siguiente.getPCB());
                        cpu.liberarCPU();
                    }
                }
            }

            System.out.println("\n--- Procesos Terminados ---");
            ColaProcesos.ColaTemporal itTerm = colaTerminados.crearIterador();
            while (itTerm.tieneSiguiente()) {
                System.out.println(itTerm.siguiente().getPCB());
            }
        }

        metricas.imprimirReporte();
        System.out.println("\nSimulación finalizada (ciclo final = " + ciclo + ").");
    }
}