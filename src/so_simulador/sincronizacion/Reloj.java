package so_simulador.sincronizacion;

public class Reloj {
    private int tiempo;

    public Reloj() {
        this.tiempo = 0;
    }

    public synchronized void tick() {
        tiempo++;
        notifyAll(); // avisa a los hilos que esperan un tick
    }

    public synchronized void esperarHasta(int t) {
        while (tiempo < t) {
            try {
                wait();
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public synchronized int getTiempo() {
        return tiempo;
    }
}
