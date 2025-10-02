package so_simulador.sincronizacion;

/**
 * Implementación básica de un semáforo sin usar librerías externas.
 * Permite sincronizar procesos/hilos simulados.
 */
public class Semaforo {
    private int valor;

    public Semaforo(int inicial) {
        if (inicial < 0) inicial = 0;
        this.valor = inicial;
    }

    /** Operación P (wait) → decrementa o bloquea si no hay recursos disponibles. */
    public synchronized void waitSem() {
        while (valor <= 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                // ignorar o propagar según el diseño
            }
        }
        valor--;
    }

    /** Operación V (signal) → incrementa y despierta un hilo bloqueado si lo hay. */
    public synchronized void signal() {
        valor++;
        notify(); // despierta a un hilo en espera
    }

    /** Devuelve el valor actual del semáforo (solo para depuración). */
    public synchronized int getValor() {
        return valor;
    }
}
