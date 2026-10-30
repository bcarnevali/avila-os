package edu.unimet.avilaos.configuracion;

import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.gui.SolicitudProceso;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;

/**
 * Todos los parametros con los que arranca una simulacion: los que pide el
 * enunciado guardar en archivo para usarlos en futuras simulaciones.
 */
public class ConfiguracionSimulacion {

    public static final int MIN_COMPUTADORES = 2;
    public static final int MAX_COMPUTADORES = 8;

    private int duracionCicloMs = 500;
    private int quantum = 3;
    private int latencia = 3;
    private int[] ramPorComputador = {256, 256};
    private TipoPolitica[] politicas = {TipoPolitica.FCFS, TipoPolitica.FCFS};
    private ListaEnlazada<SolicitudProceso> procesos = new ListaEnlazada<>();
    private ListaEnlazada<BufferInicial> buffers = new ListaEnlazada<>();

    /** Configuracion por defecto: 2 computadores de 256 con FCFS. */
    public static ConfiguracionSimulacion porDefecto() {
        return new ConfiguracionSimulacion();
    }

    /** Revisa que todo tenga sentido antes de crear el cluster. Lanza IllegalArgumentException si no. */
    public void validar() {
        int n = ramPorComputador.length;
        if (n < MIN_COMPUTADORES || n > MAX_COMPUTADORES) {
            throw new IllegalArgumentException("Debe haber entre " + MIN_COMPUTADORES + " y "
                    + MAX_COMPUTADORES + " computadores.");
        }
        if (politicas.length != n) {
            throw new IllegalArgumentException("Cada computador debe tener su política.");
        }
        if (duracionCicloMs < 50 || duracionCicloMs > 5000) {
            throw new IllegalArgumentException("La duración del ciclo debe estar entre 50 y 5000 ms.");
        }
        if (quantum < 1 || quantum > 100) {
            throw new IllegalArgumentException("El quantum debe estar entre 1 y 100.");
        }
        if (latencia < 0 || latencia > 50) {
            throw new IllegalArgumentException("La latencia debe estar entre 0 y 50 ciclos.");
        }
        for (int i = 0; i < n; i++) {
            if (ramPorComputador[i] < 16 || ramPorComputador[i] > 100000) {
                throw new IllegalArgumentException("La RAM del Computador " + (i + 1) + " debe estar entre 16 y 100000.");
            }
            if (politicas[i] == null) {
                throw new IllegalArgumentException("Falta la política del Computador " + (i + 1) + ".");
            }
        }
        for (int i = 0; i < buffers.tamano(); i++) {
            BufferInicial b = buffers.obtener(i);
            if (b.idAnfitrion() < 1 || b.idAnfitrion() > n) {
                throw new IllegalArgumentException("El buffer " + b.id() + " está en el Computador "
                        + b.idAnfitrion() + ", que no existe con " + n + " computadores.");
            }
        }
        for (int i = 0; i < procesos.tamano(); i++) {
            SolicitudProceso p = procesos.obtener(i);
            if (p.idComputador() != SolicitudProceso.AUTOMATICO && (p.idComputador() < 1 || p.idComputador() > n)) {
                throw new IllegalArgumentException("El proceso " + p.nombre() + " está asignado al Computador "
                        + p.idComputador() + ", que no existe con " + n + " computadores.");
            }
        }
    }

    public int getCantidadComputadores() { return ramPorComputador.length; }
    public int getDuracionCicloMs() { return duracionCicloMs; }
    public void setDuracionCicloMs(int ms) { this.duracionCicloMs = ms; }
    public int getQuantum() { return quantum; }
    public void setQuantum(int quantum) { this.quantum = quantum; }
    public int getLatencia() { return latencia; }
    public void setLatencia(int latencia) { this.latencia = latencia; }
    public int[] getRamPorComputador() { return ramPorComputador; }
    public TipoPolitica[] getPoliticas() { return politicas; }

    /** Cambia la cantidad de computadores y su RAM y politica. Los dos arreglos deben medir lo mismo. */
    public void setComputadores(int[] ram, TipoPolitica[] politicas) {
        if (ram.length != politicas.length) {
            throw new IllegalArgumentException("Cada computador debe tener RAM y política.");
        }
        this.ramPorComputador = ram;
        this.politicas = politicas;
    }

    public ListaEnlazada<SolicitudProceso> getProcesos() { return procesos; }
    public void setProcesos(ListaEnlazada<SolicitudProceso> procesos) { this.procesos = procesos; }
    public ListaEnlazada<BufferInicial> getBuffers() { return buffers; }
    public void setBuffers(ListaEnlazada<BufferInicial> buffers) { this.buffers = buffers; }
}
