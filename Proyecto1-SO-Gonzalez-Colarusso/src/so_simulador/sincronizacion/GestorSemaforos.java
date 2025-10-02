package so_simulador.sincronizacion;

public class GestorSemaforos {
    private static final int MAX = 50;
    private String[] nombres;
    private Semaforo[] semaforos;
    private int total;

    public GestorSemaforos() {
        nombres = new String[MAX];
        semaforos = new Semaforo[MAX];
        total = 0;
    }

    /** Crea un nuevo semáforo con nombre y valor inicial. */
    public boolean crear(String nombre, int valorInicial) {
        if (buscar(nombre) != null || total >= MAX) return false;
        nombres[total] = nombre;
        semaforos[total] = new Semaforo(valorInicial);
        total++;
        return true;
    }

    /** Obtiene el semáforo por nombre (o null si no existe). */
    public Semaforo buscar(String nombre) {
        for (int i = 0; i < total; i++) {
            if (nombres[i].equals(nombre)) return semaforos[i];
        }
        return null;
    }
}
