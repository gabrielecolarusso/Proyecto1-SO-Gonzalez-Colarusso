package so_simulador.planificador;

import so_simulador.modelo.*;

public class MLFQ implements Planificador {
    private ColaProcesos[] niveles;
    private int[] quantums;
    private int nivelesCount;
    private int agingThreshold; // ciclos de espera para promover

    public MLFQ(int nivelesCount, int[] quantums, int agingThreshold) {
        this.nivelesCount = nivelesCount;
        this.quantums = new int[nivelesCount];
        System.arraycopy(quantums, 0, this.quantums, 0, Math.min(quantums.length, nivelesCount));
        this.agingThreshold = agingThreshold;

        niveles = new ColaProcesos[nivelesCount];
        for (int i = 0; i < nivelesCount; i++) {
            niveles[i] = new ColaProcesos();
        }
    }

    // Encolar al nivel correspondiente (usado por el Main o por el propio MLFQ)
    public void encolarProceso(Proceso p) {
        int nivel = p.getMlfqNivel();
        if (nivel < 0) nivel = 0;
        if (nivel >= nivelesCount) nivel = nivelesCount - 1;
        niveles[nivel].encolar(p);
    }

    // Planificador selecciona el siguiente proceso del nivel más alto no vacío
    @Override
    public Proceso seleccionarProceso(ColaProcesos colaGeneral) {
        // Nota: colaGeneral no se usa dentro de MLFQ; MLFQ mantiene sus colas internas.
        for (int i = 0; i < nivelesCount; i++) {
            if (!niveles[i].estaVacia()) {
                return niveles[i].desencolar();
            }
        }
        return null;
    }

    @Override
    public String getNombre() {
        return "Multilevel Feedback Queue (MLFQ)";
    }

    public int getQuantumParaNivel(int nivel) {
        if (nivel < 0) nivel = 0;
        if (nivel >= quantums.length) nivel = quantums[quantums.length - 1];
        return quantums[nivel];
    }

    // aging: incrementar espera en todas las colas y promover si excede threshold
    public void aging() {
        for (int lvl = 1; lvl < nivelesCount; lvl++) { // no promote desde nivel 0
            ColaProcesos.ColaTemporal it = niveles[lvl].crearIterador();
            while (it.tieneSiguiente()) {
                Proceso p = it.siguiente();
                p.incrementarMlfqEspera();
            }
        }

        // ahora recorrer niveles y promover los que exceden threshold
        for (int lvl = 1; lvl < nivelesCount; lvl++) {
            ColaProcesos.ColaTemporal it = niveles[lvl].crearIterador();
            while (it.tieneSiguiente()) {
                Proceso p = it.siguiente();
                if (p.getMlfqTiempoEspera() >= agingThreshold) {
                    // promover: eliminar y encolar en nivel-1
                    niveles[lvl].eliminarProceso(p);
                    p.resetMlfqEspera();
                    p.promoteMlfq();
                    niveles[p.getMlfqNivel()].encolar(p);
                }
            }
        }
    }
}
