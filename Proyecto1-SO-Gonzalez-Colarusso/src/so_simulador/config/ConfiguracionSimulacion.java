package so_simulador.config;

public class ConfiguracionSimulacion {
    private int cicloDuracion; // en milisegundos
    private int memoriaTotal;  // KB
    private int memoriaPorProceso; // KB
    private String algoritmoInicial;
    private ProcesoConfig[] procesos;
    private int maximoProcesos;
    
    public ConfiguracionSimulacion() {
        this.cicloDuracion = 1000;
        this.memoriaTotal = 1024;
        this.memoriaPorProceso = 64;
        this.algoritmoInicial = "FCFS";
        this.maximoProcesos = 100;
        this.procesos = new ProcesoConfig[maximoProcesos];
    }
    
    // Getters y setters
    public int getCicloDuracion() { return cicloDuracion; }
    public void setCicloDuracion(int cicloDuracion) { 
        if (cicloDuracion > 0) this.cicloDuracion = cicloDuracion; 
    }
    
    public int getMemoriaTotal() { return memoriaTotal; }
    public void setMemoriaTotal(int memoriaTotal) { 
        if (memoriaTotal > 0) this.memoriaTotal = memoriaTotal; 
    }
    
    public int getMemoriaPorProceso() { return memoriaPorProceso; }
    public void setMemoriaPorProceso(int memoriaPorProceso) { 
        if (memoriaPorProceso > 0) this.memoriaPorProceso = memoriaPorProceso; 
    }
    
    public String getAlgoritmoInicial() { return algoritmoInicial; }
    public void setAlgoritmoInicial(String algoritmoInicial) { 
        this.algoritmoInicial = algoritmoInicial; 
    }
    
    public ProcesoConfig[] getProcesos() { return procesos; }
    
    public void agregarProceso(ProcesoConfig p) {
        for (int i = 0; i < procesos.length; i++) {
            if (procesos[i] == null) {
                procesos[i] = p;
                return;
            }
        }
    }
    
    public int contarProcesos() {
        int count = 0;
        for (ProcesoConfig p : procesos) {
            if (p != null) count++;
        }
        return count;
    }
}