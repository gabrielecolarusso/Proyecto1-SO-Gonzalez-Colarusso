package so_simulador.modelo;

public class PCB {
    private static int contadorId = 0;
    private final int id;
    private String nombre;
    private EstadoProceso estado;
    private int programCounter;
    private int memoryAddressRegister;

    public PCB(String nombre) {
        this.id = ++contadorId;
        this.nombre = nombre;
        this.estado = EstadoProceso.NUEVO;
        this.programCounter = 0;
        this.memoryAddressRegister = 0;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public EstadoProceso getEstado() { return estado; }
    public int getProgramCounter() { return programCounter; }
    public int getMemoryAddressRegister() { return memoryAddressRegister; }

    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public void incrementarPC() { this.programCounter++; }
    public void incrementarMAR() { this.memoryAddressRegister++; }

    @Override
    public String toString() {
        return String.format("PCB{id=%d, nombre=%s, estado=%s, PC=%d, MAR=%d}", 
                              id, nombre, estado, programCounter, memoryAddressRegister);
    }
}
