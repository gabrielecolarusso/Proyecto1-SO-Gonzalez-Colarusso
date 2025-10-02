package so_simulador;

import so_simulador.modelo.*;
import so_simulador.planificador.*;
import so_simulador.util.Metricas;

public class Main {
    public static void main(String[] args) {
        ColaProcesos cola = new ColaProcesos();

        // Crear procesos: (nombre, instrucciones, esCPUbound, ciclosExcepcion, ciclosAtencion, prioridad)
        cola.encolar(new Proceso("Proceso A", 5, true, 0, 0, 3));
        cola.encolar(new Proceso("Proceso B", 7, true, 0, 0, 1));
        cola.encolar(new Proceso("Proceso C", 4, true, 0, 0, 2));
        cola.encolar(new Proceso("Proceso D", 2, true, 0, 0, 4));

        // Gestor de planificadores
        GestorPlanificadores gestor = new GestorPlanificadores();
        gestor.registrar(new FCFS());
        gestor.registrar(new SJF());
        gestor.registrar(new RoundRobin(2));
        gestor.registrar(new Prioridades());
        gestor.registrar(new HRRN());

        // Crear MLFQ (3 niveles) y registrar
        int[] quantums = new int[]{1, 2, 4};
        MLFQ mlfq = new MLFQ(3, quantums, 5);
        gestor.registrar(mlfq);

        gestor.listarAlgoritmos();

        // Cambia este índice para probar otros planificadores (1..6)
        // 1=FCFS,2=SJF,3=RR,4=Prioridades,5=HRRN,6=MLFQ
        gestor.seleccionar(6); // por defecto probamos MLFQ aquí

        Planificador planificador = gestor.getActivo();
        CPU cpu = new CPU();

        System.out.println("\nUsando planificador: " + planificador.getNombre());
        System.out.println("--- Simulación con métricas ---");

        Metricas metricas = new Metricas();
        int ciclo = 0; // tiempo lógico en "instrucciones" ejecutadas

        // Registrar llegada de todos los procesos (tick = 0)
        ColaProcesos.ColaTemporal itInit = cola.crearIterador();
        while (itInit.tieneSiguiente()) {
            Proceso p = itInit.siguiente();
            metricas.registrarLlegada(p, ciclo);
        }

        // RUTA MLFQ (maneja sus propias colas internas)
        if (planificador instanceof MLFQ gestorMlfq) {
            // mover procesos de cola general a MLFQ (nivel 0)
            while (!cola.estaVacia()) {
                Proceso p = cola.desencolar();
                p.setMlfqNivel(0);
                gestorMlfq.encolarProceso(p);
            }

            while (true) {
                gestorMlfq.aging(); // aplicar aging
                Proceso siguiente = gestorMlfq.seleccionarProceso(null);
                if (siguiente == null) break;

                // registrar primer inicio si aplica
                metricas.registrarInicio(siguiente, ciclo);

                System.out.println("Ejecutando: " + siguiente.getPCB() + " (nivel=" + siguiente.getMlfqNivel() + ")");

                int q = gestorMlfq.getQuantumParaNivel(siguiente.getMlfqNivel());
                int pcAntes = siguiente.getPCB().getProgramCounter();

                // ejecutar quantum (modo secuencial)
                boolean terminado = siguiente.ejecutarQuantum(q);

                int pcDespues = siguiente.getPCB().getProgramCounter();
                int ejecutadas = pcDespues - pcAntes;
                ciclo += ejecutadas;
                metricas.registrarEjecucion(siguiente, ejecutadas, ciclo);

                if (terminado) {
                    metricas.registrarFinalizacion(siguiente, ciclo);
                    System.out.println("Finalizado: " + siguiente.getPCB());
                } else {
                    // demote si corresponde
                    if (siguiente.getMlfqNivel() < 2) siguiente.demoteMlfq();
                    gestorMlfq.encolarProceso(siguiente);
                    System.out.println("Quantum agotado → demoted a nivel " + siguiente.getMlfqNivel() + " ; reencolando: " + siguiente.getPCB());
                }
            }
        }
        // RUTA GENERAL (FCFS, SJF, RR, Prioridades, HRRN)
        else {
            while (!cola.estaVacia()) {
                // Para HRRN incrementamos espera en procesos aún en cola (ya implementado en Proceso si aplica)
                if (planificador instanceof HRRN) {
                    ColaProcesos.ColaTemporal aux = cola.crearIterador();
                    while (aux.tieneSiguiente()) aux.siguiente().incrementarEspera();
                }

                Proceso siguiente = planificador.seleccionarProceso(cola);

                // registrar inicio de ejecución (primer inicio si aplica)
                metricas.registrarInicio(siguiente, ciclo);

                cpu.cargarProceso(siguiente);
                System.out.println("Ejecutando: " + siguiente.getPCB());

                if (planificador instanceof RoundRobin rr) {
                    int pcAntes = siguiente.getPCB().getProgramCounter();
                    boolean terminado = siguiente.ejecutarQuantum(rr.getQuantum());
                    int pcDespues = siguiente.getPCB().getProgramCounter();
                    int ejecutadas = pcDespues - pcAntes;
                    ciclo += ejecutadas;
                    metricas.registrarEjecucion(siguiente, ejecutadas, ciclo);

                    if (terminado) {
                        metricas.registrarFinalizacion(siguiente, ciclo);
                        System.out.println("Finalizado: " + siguiente.getPCB());
                    } else {
                        System.out.println("Quantum terminado, regresa a cola: " + siguiente.getPCB());
                        cola.encolar(siguiente);
                    }
                }
                // HRRN y no-preemptive: ejecución completa
                else {
                    int restantes = siguiente.getServicioRestante();
                    siguiente.run();
                    ciclo += restantes;
                    metricas.registrarEjecucion(siguiente, restantes, ciclo);
                    metricas.registrarFinalizacion(siguiente, ciclo);
                    System.out.println("Finalizado: " + siguiente.getPCB());
                }

                cpu.liberarCPU();
            }
        }

        // Al finalizar simulación, imprimir métricas
        metricas.imprimirReporte();
        System.out.println("Simulación finalizada (ciclo final = " + ciclo + ").");
    }
}
