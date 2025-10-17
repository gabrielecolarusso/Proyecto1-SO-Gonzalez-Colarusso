package so_simulador.config;

public class TestConfiguracion {
    public static void main(String[] args) {
        System.out.println("=== TEST DE CONFIGURACIÓN ===\n");
        
        // 1. Crear configuración
        ConfiguracionSimulacion config = new ConfiguracionSimulacion();
        config.setCicloDuracion(500);
        config.setMemoriaTotal(2048);
        config.setMemoriaPorProceso(128);
        config.setAlgoritmoInicial("Round Robin");
        
        // Agregar procesos
        config.agregarProceso(new ProcesoConfig("Proceso A", 10, false, 0, 3, 1, 64));
        config.agregarProceso(new ProcesoConfig("Proceso B", 15, true, 0, 0, 2, 128));
        config.agregarProceso(new ProcesoConfig("Proceso C", 8, false, 0, 4, 3, 96));
        
        System.out.println("Configuración creada:");
        System.out.println("- Ciclo: " + config.getCicloDuracion() + "ms");
        System.out.println("- Memoria total: " + config.getMemoriaTotal() + "KB");
        System.out.println("- Algoritmo: " + config.getAlgoritmoInicial());
        System.out.println("- Procesos: " + config.contarProcesos());
        
        // 2. Guardar
        String archivo = "config_test.json";
        boolean guardado = JSONHandler.guardar(config, archivo);
        System.out.println("\n✓ Guardado: " + (guardado ? "ÉXITO" : "FALLO"));
        
        // 3. Cargar
        ConfiguracionSimulacion cargada = JSONHandler.cargar(archivo);
        System.out.println("\n✓ Cargado:");
        System.out.println("- Ciclo: " + cargada.getCicloDuracion() + "ms");
        System.out.println("- Memoria total: " + cargada.getMemoriaTotal() + "KB");
        System.out.println("- Algoritmo: " + cargada.getAlgoritmoInicial());
        System.out.println("- Procesos cargados: " + cargada.contarProcesos());
        
        // 4. Verificar procesos
        System.out.println("\nProcesos:");
        for (ProcesoConfig p : cargada.getProcesos()) {
            if (p != null) {
                System.out.println("  - " + p.getNombre() + 
                                 " (Inst: " + p.getInstrucciones() + 
                                 ", Mem: " + p.getMemoriaRequerida() + "KB)");
            }
        }
        
        System.out.println("\n=== TEST COMPLETADO ===");
    }
}