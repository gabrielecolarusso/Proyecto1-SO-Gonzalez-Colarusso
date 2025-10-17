package so_simulador.config;

public class ProcesoConfig {
    private String nombre;
    private int instrucciones;
    private boolean esCPUbound;
    private int ciclosExcepcion;
    private int ciclosAtencion;
    private int prioridad;
    private int memoriaRequerida; // KB
    
    public ProcesoConfig(String nombre, int instrucciones, boolean esCPUbound,
                         int ciclosExcepcion, int ciclosAtencion, int prioridad, 
                         int memoriaRequerida) {
        this.nombre = nombre;
        this.instrucciones = instrucciones;
        this.esCPUbound = esCPUbound;
        this.ciclosExcepcion = ciclosExcepcion;
        this.ciclosAtencion = ciclosAtencion;
        this.prioridad = prioridad;
        this.memoriaRequerida = memoriaRequerida;
    }
    
    // Getters
    public String getNombre() { return nombre; }
    public int getInstrucciones() { return instrucciones; }
    public boolean isEsCPUbound() { return esCPUbound; }
    public int getCiclosExcepcion() { return ciclosExcepcion; }
    public int getCiclosAtencion() { return ciclosAtencion; }
    public int getPrioridad() { return prioridad; }
    public int getMemoriaRequerida() { return memoriaRequerida; }
    
    // Setters
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setInstrucciones(int instrucciones) { this.instrucciones = instrucciones; }
    public void setEsCPUbound(boolean esCPUbound) { this.esCPUbound = esCPUbound; }
    public void setCiclosExcepcion(int ciclosExcepcion) { this.ciclosExcepcion = ciclosExcepcion; }
    public void setCiclosAtencion(int ciclosAtencion) { this.ciclosAtencion = ciclosAtencion; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
    public void setMemoriaRequerida(int memoriaRequerida) { this.memoriaRequerida = memoriaRequerida; }
}