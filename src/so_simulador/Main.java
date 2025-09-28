package so_simulador;

import so_simulador.modelo.*;
import so_simulador.planificador.*;

public class Main {
    public static void main(String[] args) {
        ColaProcesos cola = new ColaProcesos();

        // Proceso(nombre, instrucciones, esCPUbound, ciclosExcepcion, ciclosAtencion, prioridad)
        cola.encolar(new Proceso("Proceso A", 5, true, 0, 0, 3)); 
        cola.encolar(new Proceso("Proceso B", 7, true, 0, 0, 1)); 
        cola.encolar(new Proceso("Proceso C", 4, true, 0, 0, 2)); 
        cola.encolar(new Proceso("Proceso D", 2, true, 0, 0, 4)); 

        GestorPlanificadores gestor = new GestorPlanificadores();
        gestor.registrar(new FCFS());
        gestor.registrar(new SJF());
        gestor.registrar(new RoundRobin(2));
        gestor.registrar(new Prioridades());
        gestor.registrar(new HRRN());  

        // Crear MLFQ: 3 niveles con quantums {1,2,4}, agingThreshold=5
        int[] quantums = new int[]{1, 2, 4};
        MLFQ mlfq = new MLFQ(3, quantums, 5);
        gestor.registrar(mlfq);

        gestor.listarAlgoritmos();
        gestor.seleccionar(6); // Selecciona MLFQ en este ejemplo

        CPU cpu = new CPU();
        Planificador planificador = gestor.getActivo();

        System.out.println("\nUsando planificador: " + planificador.getNombre());
        System.out.println("--- Simulación ---");

        // Caso especial: MLFQ mantiene sus colas internas
        if (planificador instanceof MLFQ gestorMlfq) {
            // Inicial: mover todos los procesos de la cola general a MLFQ nivel 0
            while (!cola.estaVacia()) {
                Proceso p = cola.desencolar();
                p.setMlfqNivel(0);
                gestorMlfq.encolarProceso(p);
            }

            // Bucle principal de MLFQ
            while (true) {
                gestorMlfq.aging(); // aging en cada iteración

                Proceso siguiente = gestorMlfq.seleccionarProceso(null);
                if (siguiente == null) break;

                cpu.cargarProceso(siguiente);
                System.out.println("Ejecutando: " + siguiente.getPCB() + " (nivel=" + siguiente.getMlfqNivel() + ")");

                int quantum = gestorMlfq.getQuantumParaNivel(siguiente.getMlfqNivel());
                boolean terminado = siguiente.ejecutarQuantum(quantum);

                if (terminado) {
                    System.out.println("Finalizado: " + siguiente.getPCB());
                } else {
                    // demote si no está ya en el último nivel
                    if (siguiente.getMlfqNivel() < 2) {
                        siguiente.demoteMlfq();
                    }
                    System.out.println("Quantum agotado → demoted a nivel " + siguiente.getMlfqNivel() 
                        + " ; reencolando: " + siguiente.getPCB());
                    gestorMlfq.encolarProceso(siguiente);
                }

                cpu.liberarCPU();
            }
        } 
        // Caso normal: FCFS, SJF, RR, Prioridades, HRRN
        else {
            while (!cola.estaVacia()) {
                // HRRN: antes de elegir proceso, todos en cola incrementan tiempo de espera
                if (planificador instanceof HRRN) {
                    ColaProcesos.ColaTemporal aux = cola.crearIterador();
                    while (aux.tieneSiguiente()) {
                        aux.siguiente().incrementarEspera();
                    }
                }

                Proceso siguiente = planificador.seleccionarProceso(cola);
                cpu.cargarProceso(siguiente);
                System.out.println("Ejecutando: " + siguiente.getPCB());

                // Round Robin → quantum
                if (planificador instanceof RoundRobin rr) {
                    boolean terminado = siguiente.ejecutarQuantum(rr.getQuantum());
                    if (terminado) {
                        System.out.println("Finalizado: " + siguiente.getPCB());
                    } else {
                        System.out.println("Quantum terminado, regresa a cola: " + siguiente.getPCB());
                        cola.encolar(siguiente);
                    }
                }
                // HRRN → ejecución completa
                else if (planificador instanceof HRRN) {
                    siguiente.resetEspera(); // Reset de espera al ser elegido
                    siguiente.run();
                    System.out.println("Finalizado: " + siguiente.getPCB());
                }
                // Otros planificadores (FCFS, SJF, Prioridades) → ejecución completa
                else {
                    siguiente.run();
                    System.out.println("Finalizado: " + siguiente.getPCB());
                }

                cpu.liberarCPU();
            }
        }
    }
}
