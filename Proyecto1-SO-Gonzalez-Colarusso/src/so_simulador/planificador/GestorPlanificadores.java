package so_simulador.planificador;

public class GestorPlanificadores {
    private Planificador[] algoritmos;
    private int total;
    private Planificador activo;

    public GestorPlanificadores() {
        // Máximo 10 planificadores por simplicidad
        this.algoritmos = new Planificador[10];
        this.total = 0;
        this.activo = null;
    }

    public void registrar(Planificador p) {
        if (total < algoritmos.length) {
            algoritmos[total] = p;
            total++;
            if (activo == null) {
                activo = p; // El primero registrado queda como activo
            }
        }
    }

    public void listarAlgoritmos() {
        System.out.println("Algoritmos disponibles:");
        for (int i = 0; i < total; i++) {
            System.out.println((i + 1) + ". " + algoritmos[i].getNombre());
        }
    }

    public void seleccionar(int indice) {
        if (indice >= 1 && indice <= total) {
            activo = algoritmos[indice - 1];
            System.out.println("Planificador activo: " + activo.getNombre());
        } else {
            System.out.println("Índice inválido.");
        }
    }

    public Planificador getActivo() {
        return activo;
    }
}