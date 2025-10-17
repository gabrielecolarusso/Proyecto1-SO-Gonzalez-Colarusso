package so_simulador.memoria;

import so_simulador.modelo.*;

public class TestMemoria {
    public static void main(String[] args) {
        System.out.println("=== TEST DE GESTIÓN DE MEMORIA Y SUSPENSIÓN ===\n");
        
        // 1. Crear gestor de memoria (1024 KB total, 128 KB por proceso = max 8 procesos)
        GestorMemoria gestor = new GestorMemoria(1024, 128);
        System.out.println("Gestor creado: " + gestor.getEstadisticas());
        System.out.println("Capacidad máxima: " + gestor.getMaxProcesos() + " procesos\n");
        
        // 2. Crear colas multinivel
        ColasMultinivel colas = new ColasMultinivel(gestor);
        
        // 3. Crear 10 procesos (más de los que caben en memoria)
        System.out.println("--- Admitiendo 10 procesos al sistema ---");
        for (int i = 1; i <= 10; i++) {
            Proceso p = new Proceso("Proceso " + i, 5 + i, i % 2 == 0, 0, 3, i, 128);
            colas.admitirProceso(p);
            System.out.println("✓ " + p.getPCB().getNombre() + " admitido (estado: " + 
                             p.getPCB().getEstado() + ")");
        }
        
        System.out.println("\n" + colas.getResumen());
        System.out.println(gestor.getEstadisticas() + "\n");
        
        // 4. Planificación de largo plazo (cargar en memoria)
        System.out.println("--- Planificación de Largo Plazo (ciclo 0) ---");
        int cargados = colas.planificarLargoPlazo(0);
        System.out.println("✓ Procesos cargados en memoria: " + cargados);
        System.out.println(colas.getResumen());
        System.out.println(gestor.getEstadisticas() + "\n");
        
        // 5. Simular que algunos procesos se bloquean
        System.out.println("--- Bloqueando algunos procesos ---");
        ColaProcesos.ColaTemporal it = colas.getColaCortoPlazo().crearIterador();
        int bloqueados = 0;
        while (it.tieneSiguiente() && bloqueados < 2) {
            Proceso p = it.siguiente();
            colas.bloquearProceso(p);
            System.out.println("✓ " + p.getPCB().getNombre() + " → BLOQUEADO");
            bloqueados++;
        }
        System.out.println(colas.getResumen() + "\n");
        
        // 6. Intentar cargar más procesos (debería llenar memoria)
        System.out.println("--- Intentando cargar más procesos (ciclo 5) ---");
        cargados = colas.planificarLargoPlazo(5);
        System.out.println("✓ Procesos adicionales cargados: " + cargados);
        System.out.println(colas.getResumen());
        System.out.println(gestor.getEstadisticas() + "\n");
        
        // 7. Realizar swapping para liberar memoria
        System.out.println("--- Realizando SWAP (política: " + gestor.getPolitica() + ") ---");
        Proceso victima = colas.realizarSwap(10);
        if (victima != null) {
            System.out.println("✓ Proceso suspendido: " + victima.getPCB().getNombre() + 
                             " (estado: " + victima.getPCB().getEstado() + ")");
        } else {
            System.out.println("✗ No se pudo realizar swap");
        }
        System.out.println(colas.getResumen());
        System.out.println(gestor.getEstadisticas() + "\n");
        
        // 8. Ahora sí debe haber espacio para otro proceso
        System.out.println("--- Cargando procesos pendientes después del swap ---");
        cargados = colas.planificarLargoPlazo(15);
        System.out.println("✓ Procesos cargados: " + cargados);
        System.out.println(colas.getResumen());
        System.out.println(gestor.getEstadisticas() + "\n");
        
        // 9. Probar planificador de mediano plazo (reanudar suspendidos)
        System.out.println("--- Liberando memoria y reanudando suspendidos ---");
        // Simular que terminan algunos procesos
        it = colas.getColaCortoPlazo().crearIterador();
        int liberados = 0;
        while (it.tieneSiguiente() && liberados < 3) {
            Proceso p = it.siguiente();
            gestor.liberarProceso(p);
            System.out.println("✓ " + p.getPCB().getNombre() + " terminó (memoria liberada)");
            liberados++;
        }
        
        System.out.println(gestor.getEstadisticas());
        
        int reanudados = colas.planificarMedianoPlazo(20);
        System.out.println("✓ Procesos reanudados desde suspendido: " + reanudados);
        System.out.println(colas.getResumen());
        System.out.println(gestor.getEstadisticas() + "\n");
        
        // 10. Probar diferentes políticas de swap
        System.out.println("--- Probando política PRIORIDAD ---");
        gestor.setPolitica(GestorMemoria.PoliticaSwap.PRIORIDAD);
        victima = colas.realizarSwap(25);
        if (victima != null) {
            System.out.println("✓ Suspendido (por prioridad): " + victima.getPCB().getNombre() + 
                             " (prioridad: " + victima.getPrioridad() + ")");
        }
        System.out.println(colas.getResumen() + "\n");
        
        System.out.println("=== TEST COMPLETADO ===");
    }
}