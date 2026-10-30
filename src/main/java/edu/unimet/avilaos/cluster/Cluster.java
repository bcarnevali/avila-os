package edu.unimet.avilaos.cluster;

import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.hardware.RelojGlobal;
import edu.unimet.avilaos.kernel.RegistroEventos;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import edu.unimet.avilaos.sincronizacion.Buffer;

/**
 *
 * @author mfbal
 */
public class Cluster {
    private final ListaEnlazada<Computador> computadores = new ListaEnlazada<>();
    private final ListaEnlazada<Buffer> buffers = new ListaEnlazada<>();
    private final RelojGlobal reloj;
    private final RedInterconexion red;
    /** Latencia de red por defecto, en ciclos. */
    public static final int LATENCIA_INICIAL = 3;
    /** Todos los computadores con la misma politica y la latencia por defecto. */
    public Cluster(int[] ramPorComputador, TipoPolitica politica, int quantum, long duracionCicloMs, RegistroEventos log) {
        this(ramPorComputador, mismaPolitica(politica, ramPorComputador == null ? 0 : ramPorComputador.length),
                quantum, duracionCicloMs, LATENCIA_INICIAL, log);
    }

    /** Cada computador con su propia RAM y su propia politica inicial. */
    public Cluster(int[] ramPorComputador, TipoPolitica[] politicas, int quantum, long duracionCicloMs,
                   int latencia, RegistroEventos log) {
        if (ramPorComputador == null || ramPorComputador.length < 2) {
            throw new IllegalArgumentException("Se necesitan al menos 2 computadores");
        }
        if (politicas == null || politicas.length != ramPorComputador.length) {
            throw new IllegalArgumentException("Cada computador necesita su politica");
        }
        this.reloj = new RelojGlobal(duracionCicloMs);
        // La red va primero: los procesos que llegan quedan listos antes del tick de su computador
        this.red = new RedInterconexion(this, latencia, log);
        reloj.registrar(red);
        for (int i = 0; i < ramPorComputador.length; i++) {
            Computador c = new Computador(i + 1, ramPorComputador[i], politicas[i], quantum, log);
            c.setCluster(this); // Vincula el computador con el cluster
            computadores.agregar(c);
            reloj.registrar(c);
        }
    }

    private static TipoPolitica[] mismaPolitica(TipoPolitica politica, int cantidad) {
        TipoPolitica[] arreglo = new TipoPolitica[cantidad];
        for (int i = 0; i < cantidad; i++) {
            arreglo[i] = politica;
        }
        return arreglo;
    }

    public Computador getComputador(int id) {
        if (id < 1 || id > computadores.tamano()) {
            return null;
        }
        return computadores.obtener(id - 1);
    }

    public ListaEnlazada<Computador> getComputadores() {
        return computadores;
    }

    public int getCantidad() {
        return computadores.tamano();
    }

    public RelojGlobal getReloj() {
        return reloj;
    }

    public RedInterconexion getRed() {
        return red;
    }

    public void iniciar() {
        reloj.start();
        reloj.reanudar();
    }

    // ===================== Gestión de Búferes =====================

    public Buffer crearBuffer(String id, int capacidad, int idAnfitrion) {
        Computador comp = getComputador(idAnfitrion);
        if (comp == null) {
            throw new IllegalArgumentException("No existe el computador con ID " + idAnfitrion);
        }
        Buffer b = new Buffer(id, capacidad, idAnfitrion, comp.getMemoria());
        buffers.agregar(b);
        return b;
    }

    public Buffer getBuffer(String id) {
        if (id == null) return null;
        for (int i = 0; i < buffers.tamano(); i++) {
            Buffer b = buffers.obtener(i);
            if (id.equals(b.getId())) {
                return b;
            }
        }
        return null;
    }

    public Buffer[] getBuffers() {
        int n = buffers.tamano();
        Buffer[] copia = new Buffer[n];
        for (int i = 0; i < n; i++) {
            copia[i] = buffers.obtener(i);
        }
        return copia;
    }
}