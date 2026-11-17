package edu.unimet.avilaos.disco;

import edu.unimet.avilaos.disco.politicas.FabricaPoliticasDisco;
import edu.unimet.avilaos.disco.politicas.PoliticaDisco;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.kernel.RegistroEventos;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import java.util.concurrent.Semaphore;

/**
 * Planificador del disco: la cola única de E/S y el cabezal.
 *
 * En cada ciclo de reloj el disco hace UNA de estas cosas:
 *   - transfiere un bloque de la solicitud en curso (cada bloque cuesta un ciclo), o
 *   - mueve el cabezal hasta "velocidad" posiciones hacia su destino, o
 *   - si no hay nada que atender, se queda quieto.
 * Elegir la próxima solicitud no cuesta ciclos. El disco atiende una sola
 * solicitud a la vez y no la cambia hasta terminarla.
 *
 * Con SCAN y C-SCAN, si no queda nada en la dirección del cabezal, este sigue
 * hasta el extremo del disco; ahí SCAN invierte la dirección y C-SCAN salta al
 * extremo opuesto (el salto no cuenta como desplazamiento).
 *
 * El hilo del reloj llama a avanzar() y el de la interfaz lee el estado o cambia
 * la política, así que todo está protegido con un Semaphore de Java.
 */
public class PlanificadorDisco {

    private final int totalBloques;
    private final RegistroEventos log;
    private final Semaphore mutex = new Semaphore(1);

    private final ListaEnlazada<SolicitudES> cola = new ListaEnlazada<>();
    private PoliticaDisco politica;
    private int posicion;
    private DireccionCabezal direccion;
    private int velocidad;

    private SolicitudES actual;
    private boolean transfiriendo;
    private int bloquesRestantes;

    // Métricas
    private long desplazamientoTotal;
    private int atendidas;
    private long sumaEsperas;
    private final int[] atendidasPorPolitica = new int[TipoPoliticaDisco.values().length];

    public PlanificadorDisco(int totalBloques, int posicionInicial, DireccionCabezal direccionInicial,
                             int velocidad, TipoPoliticaDisco politicaInicial, RegistroEventos log) {
        if (totalBloques < 1) {
            throw new IllegalArgumentException("El disco debe tener al menos un bloque");
        }
        if (posicionInicial < 0 || posicionInicial >= totalBloques) {
            throw new IllegalArgumentException("La posición inicial del cabezal debe estar entre 0 y "
                    + (totalBloques - 1));
        }
        if (velocidad < 1) {
            throw new IllegalArgumentException("El cabezal debe moverse al menos una posición por ciclo");
        }
        this.totalBloques = totalBloques;
        this.posicion = posicionInicial;
        this.direccion = direccionInicial != null ? direccionInicial : DireccionCabezal.ARRIBA;
        this.velocidad = velocidad;
        this.politica = FabricaPoliticasDisco.crear(politicaInicial);
        this.log = log != null ? log : (ciclo, mensaje) -> { };
    }

    // ===================== Operación =====================

    /** Agrega una solicitud al final de la cola de E/S. */
    public void encolar(SolicitudES solicitud, long ciclo) {
        if (solicitud.getBloque() >= totalBloques) {
            throw new IllegalArgumentException("El bloque " + solicitud.getBloque() + " no existe en el disco");
        }
        bloquear();
        try {
            cola.agregar(solicitud);
        } finally {
            mutex.release();
        }
        log.registrar(ciclo, "[Disco] Llega a la cola de E/S: " + solicitud);
    }

    /**
     * Avanza el disco un ciclo de reloj.
     *
     * @return la solicitud que terminó en este ciclo, o null si ninguna terminó
     */
    public SolicitudES avanzar(long ciclo) {
        bloquear();
        try {
            if (actual == null && !elegirSiguiente(ciclo)) {
                return null; // cola vacía: el disco queda ocioso
            }
            if (actual == null) {
                barrerHaciaElExtremo(ciclo);
                return null;
            }
            if (!transfiriendo) {
                moverHacia(actual.getBloque());
                if (posicion != actual.getBloque()) {
                    return null; // todavía viajando
                }
                transfiriendo = true;
                bloquesRestantes = actual.getBloquesATransferir();
                return null; // la transferencia empieza en el próximo ciclo
            }
            bloquesRestantes--;
            if (bloquesRestantes > 0) {
                return null;
            }
            return terminarActual(ciclo);
        } finally {
            mutex.release();
        }
    }

    /**
     * Le pregunta a la política cuál atender. Devuelve false si la cola está vacía.
     * Si la política no elige ninguna (SCAN/C-SCAN sin nada adelante), deja actual en null.
     */
    private boolean elegirSiguiente(long ciclo) {
        if (cola.estaVacia()) {
            return false;
        }
        int indice = politica.elegir(cola, posicion, direccion);
        if (indice < 0 && llegoAlExtremo()) {
            cambiarDeSentidoEnElExtremo(ciclo);
            indice = politica.elegir(cola, posicion, direccion);
        }
        if (indice < 0) {
            return true; // hay solicitudes, pero primero se barre hasta el extremo
        }
        actual = cola.obtener(indice);
        cola.eliminar(actual);
        actual.setCicloInicio(ciclo);
        transfiriendo = false;
        sumaEsperas += actual.getEspera();
        atendidasPorPolitica[politica.getTipo().ordinal()]++;
        log.registrar(ciclo, "[Disco] Atiende la solicitud de " + actual.getSolicitante()
                + " en el bloque " + actual.getBloque() + " (" + nombre(politica.getTipo())
                + "), cabezal en " + posicion);
        // si el cabezal ya está en el bloque, la transferencia empieza en este mismo ciclo
        if (posicion == actual.getBloque()) {
            transfiriendo = true;
            bloquesRestantes = actual.getBloquesATransferir();
        }
        return true;
    }

    private void barrerHaciaElExtremo(long ciclo) {
        moverHacia(extremoActual());
        if (llegoAlExtremo()) {
            log.registrar(ciclo, "[Disco] No hay solicitudes adelante; el cabezal llega al extremo (bloque "
                    + posicion + ")");
        }
    }

    private boolean llegoAlExtremo() {
        return posicion == extremoActual();
    }

    private int extremoActual() {
        return direccion == DireccionCabezal.ARRIBA ? totalBloques - 1 : 0;
    }

    private void cambiarDeSentidoEnElExtremo(long ciclo) {
        if (politica.saltaAlExtremoOpuesto()) {
            int destino = direccion == DireccionCabezal.ARRIBA ? 0 : totalBloques - 1;
            log.registrar(ciclo, "[Disco] El cabezal llega al bloque " + posicion
                    + " y salta al " + destino + " (" + nombre(politica.getTipo()) + ")");
            posicion = destino; // el salto no cuenta como desplazamiento
        } else {
            direccion = direccion.opuesta();
            log.registrar(ciclo, "[Disco] El cabezal llega al bloque " + posicion
                    + " y cambia de dirección hacia " + (direccion == DireccionCabezal.ARRIBA
                    ? "los bloques mayores" : "los bloques menores") + " (" + nombre(politica.getTipo()) + ")");
        }
    }

    /** Mueve el cabezal hasta "velocidad" posiciones hacia el destino y suma el desplazamiento. */
    private void moverHacia(int destino) {
        int distancia = destino - posicion;
        if (distancia == 0) {
            return;
        }
        int paso = Math.min(velocidad, Math.abs(distancia));
        if (distancia > 0) {
            posicion += paso;
            direccion = DireccionCabezal.ARRIBA;
        } else {
            posicion -= paso;
            direccion = DireccionCabezal.ABAJO;
        }
        desplazamientoTotal += paso;
    }

    private SolicitudES terminarActual(long ciclo) {
        SolicitudES terminada = actual;
        terminada.setCicloFin(ciclo);
        actual = null;
        transfiriendo = false;
        atendidas++;
        log.registrar(ciclo, "[Disco] Termina " + terminada.getOperacion() + " de "
                + terminada.getSolicitante() + " en el bloque " + terminada.getBloque());
        return terminada;
    }

    // ===================== Cambios en caliente =====================

    /** Cambia la política mientras corre la simulación. La solicitud en curso se termina igual. */
    public void cambiarPolitica(TipoPoliticaDisco tipo, long ciclo) {
        PoliticaDisco nueva = FabricaPoliticasDisco.crear(tipo);
        bloquear();
        try {
            if (politica.getTipo() == tipo) {
                return;
            }
            politica = nueva;
        } finally {
            mutex.release();
        }
        log.registrar(ciclo, "[Disco] Cambia su política a " + nombre(tipo));
    }

    public void setVelocidad(int velocidad) {
        if (velocidad < 1) {
            throw new IllegalArgumentException("El cabezal debe moverse al menos una posición por ciclo");
        }
        bloquear();
        try {
            this.velocidad = velocidad;
        } finally {
            mutex.release();
        }
    }

    // ===================== Consultas (para la interfaz y las métricas) =====================

    public int getTotalBloques() { return totalBloques; }

    public int getPosicion() {
        bloquear();
        try { return posicion; } finally { mutex.release(); }
    }

    public DireccionCabezal getDireccion() {
        bloquear();
        try { return direccion; } finally { mutex.release(); }
    }

    public int getVelocidad() {
        bloquear();
        try { return velocidad; } finally { mutex.release(); }
    }

    public TipoPoliticaDisco getPolitica() {
        bloquear();
        try { return politica.getTipo(); } finally { mutex.release(); }
    }

    /** La solicitud que el disco está atendiendo, o null. */
    public SolicitudES getActual() {
        bloquear();
        try { return actual; } finally { mutex.release(); }
    }

    /** Copia de la cola de E/S, en orden de llegada. */
    public ListaEnlazada<SolicitudES> getCola() {
        bloquear();
        try {
            ListaEnlazada<SolicitudES> copia = new ListaEnlazada<>();
            for (int i = 0; i < cola.tamano(); i++) {
                copia.agregar(cola.obtener(i));
            }
            return copia;
        } finally {
            mutex.release();
        }
    }

    public boolean estaOcioso() {
        bloquear();
        try { return actual == null && cola.estaVacia(); } finally { mutex.release(); }
    }

    public long getDesplazamientoTotal() {
        bloquear();
        try { return desplazamientoTotal; } finally { mutex.release(); }
    }

    public int getAtendidas() {
        bloquear();
        try { return atendidas; } finally { mutex.release(); }
    }

    /** Ciclos promedio que esperaron las solicitudes en la cola antes de ser elegidas. */
    public double getEsperaPromedio() {
        bloquear();
        try {
            int elegidas = 0;
            for (int n : atendidasPorPolitica) {
                elegidas += n;
            }
            return elegidas == 0 ? 0 : (double) sumaEsperas / elegidas;
        } finally {
            mutex.release();
        }
    }

    public int getAtendidasCon(TipoPoliticaDisco tipo) {
        bloquear();
        try { return atendidasPorPolitica[tipo.ordinal()]; } finally { mutex.release(); }
    }

    // ===================== Auxiliares =====================

    private void bloquear() {
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido esperando el disco", e);
        }
    }

    private static String nombre(TipoPoliticaDisco tipo) {
        return tipo == TipoPoliticaDisco.C_SCAN ? "C-SCAN" : tipo.name();
    }
}
