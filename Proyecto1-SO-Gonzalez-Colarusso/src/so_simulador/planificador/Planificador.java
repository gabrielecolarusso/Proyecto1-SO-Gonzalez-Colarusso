package so_simulador.planificador;

import so_simulador.modelo.*;

public interface Planificador {
    // Selecciona el siguiente proceso a ejecutar según la política
    Proceso seleccionarProceso(ColaProcesos cola);

    // Nombre del algoritmo (para mostrar en interfaz/logs)
    String getNombre();
}