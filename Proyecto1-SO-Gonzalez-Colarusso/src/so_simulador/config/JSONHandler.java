package so_simulador.config;

import java.io.*;

public class JSONHandler {
    
    /**
     * Guarda la configuración en formato JSON manual
     */
    public static boolean guardar(ConfiguracionSimulacion config, String rutaArchivo) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(rutaArchivo))) {
            writer.println("{");
            writer.println("  \"cicloDuracion\": " + config.getCicloDuracion() + ",");
            writer.println("  \"memoriaTotal\": " + config.getMemoriaTotal() + ",");
            writer.println("  \"memoriaPorProceso\": " + config.getMemoriaPorProceso() + ",");
            writer.println("  \"algoritmoInicial\": \"" + config.getAlgoritmoInicial() + "\",");
            writer.println("  \"procesos\": [");
            
            ProcesoConfig[] procesos = config.getProcesos();
            boolean primero = true;
            for (ProcesoConfig p : procesos) {
                if (p != null) {
                    if (!primero) writer.println(",");
                    writer.println("    {");
                    writer.println("      \"nombre\": \"" + p.getNombre() + "\",");
                    writer.println("      \"instrucciones\": " + p.getInstrucciones() + ",");
                    writer.println("      \"esCPUbound\": " + p.isEsCPUbound() + ",");
                    writer.println("      \"ciclosExcepcion\": " + p.getCiclosExcepcion() + ",");
                    writer.println("      \"ciclosAtencion\": " + p.getCiclosAtencion() + ",");
                    writer.println("      \"prioridad\": " + p.getPrioridad() + ",");
                    writer.println("      \"memoriaRequerida\": " + p.getMemoriaRequerida());
                    writer.print("    }");
                    primero = false;
                }
            }
            
            writer.println();
            writer.println("  ]");
            writer.println("}");
            
            return true;
        } catch (IOException e) {
            System.err.println("Error al guardar configuración: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Carga la configuración desde un archivo JSON
     */
    public static ConfiguracionSimulacion cargar(String rutaArchivo) {
        ConfiguracionSimulacion config = new ConfiguracionSimulacion();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            ProcesoConfig procesoActual = null;
            String nombre = null;
            int instrucciones = 0, ciclosExc = 0, ciclosAten = 0, prioridad = 0, memoria = 0;
            boolean esCPU = true;
            
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();
                
                // Parse cicloDuracion
                if (linea.startsWith("\"cicloDuracion\":")) {
                    String valor = extraerValor(linea);
                    config.setCicloDuracion(Integer.parseInt(valor));
                }
                // Parse memoriaTotal
                else if (linea.startsWith("\"memoriaTotal\":")) {
                    String valor = extraerValor(linea);
                    config.setMemoriaTotal(Integer.parseInt(valor));
                }
                // Parse memoriaPorProceso
                else if (linea.startsWith("\"memoriaPorProceso\":")) {
                    String valor = extraerValor(linea);
                    config.setMemoriaPorProceso(Integer.parseInt(valor));
                }
                // Parse algoritmoInicial
                else if (linea.startsWith("\"algoritmoInicial\":")) {
                    String valor = extraerValorString(linea);
                    config.setAlgoritmoInicial(valor);
                }
                // Parse proceso
                else if (linea.startsWith("\"nombre\":")) {
                    nombre = extraerValorString(linea);
                }
                else if (linea.startsWith("\"instrucciones\":")) {
                    instrucciones = Integer.parseInt(extraerValor(linea));
                }
                else if (linea.startsWith("\"esCPUbound\":")) {
                    esCPU = Boolean.parseBoolean(extraerValor(linea));
                }
                else if (linea.startsWith("\"ciclosExcepcion\":")) {
                    ciclosExc = Integer.parseInt(extraerValor(linea));
                }
                else if (linea.startsWith("\"ciclosAtencion\":")) {
                    ciclosAten = Integer.parseInt(extraerValor(linea));
                }
                else if (linea.startsWith("\"prioridad\":")) {
                    prioridad = Integer.parseInt(extraerValor(linea));
                }
                else if (linea.startsWith("\"memoriaRequerida\":")) {
                    memoria = Integer.parseInt(extraerValor(linea));
                    
                    // Crear proceso cuando tenemos todos los datos
                    if (nombre != null) {
                        ProcesoConfig p = new ProcesoConfig(nombre, instrucciones, esCPU,
                                ciclosExc, ciclosAten, prioridad, memoria);
                        config.agregarProceso(p);
                        nombre = null; // reset
                    }
                }
            }
            
            return config;
            
        } catch (IOException e) {
            System.err.println("Error al cargar configuración: " + e.getMessage());
            return config; // retorna configuración por defecto
        }
    }
    
    /**
     * Extrae el valor numérico o booleano de una línea JSON
     */
    private static String extraerValor(String linea) {
        int inicio = linea.indexOf(':') + 1;
        String valor = linea.substring(inicio).trim();
        // Remover coma final si existe
        if (valor.endsWith(",")) {
            valor = valor.substring(0, valor.length() - 1);
        }
        return valor.trim();
    }
    
    /**
     * Extrae el valor string de una línea JSON
     */
    private static String extraerValorString(String linea) {
        int inicio = linea.indexOf('"', linea.indexOf(':')) + 1;
        int fin = linea.indexOf('"', inicio);
        return linea.substring(inicio, fin);
    }
}