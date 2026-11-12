package edu.unimet.avilaos.disco;

import edu.unimet.avilaos.hardware.ComponenteReloj;
import edu.unimet.avilaos.kernel.RegistroEventos;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import java.util.concurrent.Semaphore;

/**
 * Disco secundario (SD) de ÁvilaOS.
 *
 * Tiene un mapa de bloques numerados de 0 a totalBloques-1 con asignación
 * encadenada: cada bloque ocupado guarda el número del siguiente bloque de su
 * archivo (o -1 si es el último), así un archivo no necesita bloques contiguos.
 *
 * Parte del disco se reserva como área de intercambio (swap) para los procesos
 * suspendidos. Como los archivos pueden estar repartidos por todo el disco, el
 * swap no es una zona fija: es un cupo de bloques que los archivos no pueden usar.
 * Un proceso suspendido toma bloques libres hasta llenar ese cupo.
 *
 * Es un componente del reloj global: en cada ciclo el planificador del disco
 * avanza el cabezal o transfiere un bloque, y cuando termina una solicitud se
 * avisa al receptor (el sistema operativo) para que desbloquee al proceso.
 */
public class Disco implements ComponenteReloj {

    /** Recibe las solicitudes de E/S que el disco termina. */
    public interface ReceptorDisco {
        void solicitudTerminada(SolicitudES solicitud, long ciclo);
    }

    public static final int MIN_BLOQUES = 100;
    public static final int MAX_BLOQUES = 500;
    public static final int SIN_SIGUIENTE = -1;

    private final int totalBloques;
    private final int tamanoSwap;
    private final PlanificadorDisco planificador;
    private final Semaphore mutex = new Semaphore(1);

    /** Siguiente bloque de la cadena, o SIN_SIGUIENTE. Solo tiene sentido si el bloque está ocupado. */
    private final int[] siguiente;
    /** Quién ocupa cada bloque (ruta del archivo o proceso suspendido); null si está libre. */
    private final String[] ocupante;
    /** true si el bloque lo ocupa un proceso suspendido (swap). */
    private final boolean[] esSwap;
    private int bloquesLibres;
    private int bloquesSwapUsados;

    private ReceptorDisco receptor;

    public Disco(int totalBloques, int tamanoSwap, int posicionCabezal, DireccionCabezal direccion,
                 int velocidadCabezal, TipoPoliticaDisco politica, RegistroEventos log) {
        if (totalBloques < MIN_BLOQUES || totalBloques > MAX_BLOQUES) {
            throw new IllegalArgumentException("El disco debe tener entre " + MIN_BLOQUES + " y "
                    + MAX_BLOQUES + " bloques");
        }
        if (tamanoSwap < 0 || tamanoSwap >= totalBloques) {
            throw new IllegalArgumentException("El área de intercambio debe tener entre 0 y "
                    + (totalBloques - 1) + " bloques");
        }
        this.totalBloques = totalBloques;
        this.tamanoSwap = tamanoSwap;
        this.siguiente = new int[totalBloques];
        this.ocupante = new String[totalBloques];
        this.esSwap = new boolean[totalBloques];
        this.bloquesLibres = totalBloques;
        this.planificador = new PlanificadorDisco(totalBloques, posicionCabezal, direccion,
                velocidadCabezal, politica, log);
    }

    public void setReceptor(ReceptorDisco receptor) {
        this.receptor = receptor;
    }

    // ===================== Reloj =====================

    @Override
    public void tick(long cicloActual) {
        SolicitudES terminada = planificador.avanzar(cicloActual);
        if (terminada != null && receptor != null) {
            receptor.solicitudTerminada(terminada, cicloActual);
        }
    }

    // ===================== Archivos =====================

    /**
     * Asigna una cadena de bloques libres a un archivo.
     *
     * @return el primer bloque de la cadena
     * @throws IllegalStateException si no hay espacio (sin contar el cupo del swap)
     */
    public int asignarArchivo(String ruta, int cantidad) {
        return asignarArchivoDesde(-1, ruta, cantidad);
    }

    /**
     * Asigna un archivo que debe empezar en un bloque dado (al cargar el sistema de
     * archivos desde JSON). El resto de la cadena se toma de los bloques libres.
     */
    public int asignarArchivoEn(int primerBloque, String ruta, int cantidad) {
        if (primerBloque < 0 || primerBloque >= totalBloques) {
            throw new IllegalArgumentException("El bloque " + primerBloque + " no existe en el disco");
        }
        return asignarArchivoDesde(primerBloque, ruta, cantidad);
    }

    private int asignarArchivoDesde(int primerBloque, String ruta, int cantidad) {
        validarCantidad(cantidad);
        bloquear();
        try {
            int disponibles = bloquesLibres - (tamanoSwap - bloquesSwapUsados);
            if (cantidad > disponibles) {
                throw new IllegalStateException("No hay espacio en el disco para " + ruta + ": necesita "
                        + cantidad + " bloques y hay " + Math.max(0, disponibles) + " disponibles");
            }
            if (primerBloque >= 0 && ocupante[primerBloque] != null) {
                throw new IllegalStateException("El bloque " + primerBloque + " ya está ocupado por "
                        + ocupante[primerBloque]);
            }
            return encadenar(primerBloque, ruta, cantidad, false);
        } finally {
            mutex.release();
        }
    }

    /** Agrega bloques al final de la cadena de un archivo (cuando crece al actualizarlo). */
    public void agregarBloques(int primerBloque, int cantidad) {
        validarCantidad(cantidad);
        bloquear();
        try {
            validarInicioDeCadena(primerBloque);
            int disponibles = bloquesLibres - (tamanoSwap - bloquesSwapUsados);
            if (cantidad > disponibles) {
                throw new IllegalStateException("No hay espacio en el disco: se necesitan " + cantidad
                        + " bloques y hay " + Math.max(0, disponibles) + " disponibles");
            }
            int ultimo = primerBloque;
            while (siguiente[ultimo] != SIN_SIGUIENTE) {
                ultimo = siguiente[ultimo];
            }
            int nuevo = encadenar(-1, ocupante[primerBloque], cantidad, false);
            siguiente[ultimo] = nuevo;
        } finally {
            mutex.release();
        }
    }

    /** Cambia el nombre que aparece como ocupante en toda la cadena (cuando se renombra un archivo o su directorio). */
    public void renombrarOcupante(int primerBloque, String nuevoNombre) {
        bloquear();
        try {
            validarInicioDeCadena(primerBloque);
            for (int b = primerBloque; b != SIN_SIGUIENTE; b = siguiente[b]) {
                ocupante[b] = nuevoNombre;
            }
        } finally {
            mutex.release();
        }
    }

    // ===================== Swap =====================

    /** true si caben "cantidad" bloques más en el área de intercambio. */
    public boolean cabeEnSwap(int cantidad) {
        bloquear();
        try {
            return bloquesSwapUsados + cantidad <= tamanoSwap && cantidad <= bloquesLibres;
        } finally {
            mutex.release();
        }
    }

    /**
     * Reserva en el swap tantos bloques como memoria tiene el proceso suspendido.
     *
     * @return el primer bloque de la cadena
     * @throws IllegalStateException si el área de intercambio está llena
     */
    public int asignarSwap(String proceso, int cantidad) {
        validarCantidad(cantidad);
        bloquear();
        try {
            if (bloquesSwapUsados + cantidad > tamanoSwap || cantidad > bloquesLibres) {
                throw new IllegalStateException("El área de intercambio no tiene espacio para " + proceso
                        + ": necesita " + cantidad + " bloques y quedan " + (tamanoSwap - bloquesSwapUsados));
            }
            int primero = encadenar(-1, proceso, cantidad, true);
            bloquesSwapUsados += cantidad;
            return primero;
        } finally {
            mutex.release();
        }
    }

    // ===================== Liberar =====================

    /** Libera toda la cadena que empieza en primerBloque (archivo eliminado o proceso reactivado). */
    public void liberar(int primerBloque) {
        bloquear();
        try {
            validarInicioDeCadena(primerBloque);
            int bloque = primerBloque;
            while (bloque != SIN_SIGUIENTE) {
                int sig = siguiente[bloque];
                if (esSwap[bloque]) {
                    bloquesSwapUsados--;
                }
                ocupante[bloque] = null;
                esSwap[bloque] = false;
                siguiente[bloque] = SIN_SIGUIENTE;
                bloquesLibres++;
                bloque = sig;
            }
        } finally {
            mutex.release();
        }
    }

    // ===================== Consultas =====================

    /** Los bloques de la cadena en orden. */
    public int[] getCadena(int primerBloque) {
        bloquear();
        try {
            validarInicioDeCadena(primerBloque);
            int largo = 0;
            for (int b = primerBloque; b != SIN_SIGUIENTE; b = siguiente[b]) {
                largo++;
            }
            int[] cadena = new int[largo];
            int i = 0;
            for (int b = primerBloque; b != SIN_SIGUIENTE; b = siguiente[b]) {
                cadena[i++] = b;
            }
            return cadena;
        } finally {
            mutex.release();
        }
    }

    public String getOcupante(int bloque) {
        bloquear();
        try { return ocupante[bloque]; } finally { mutex.release(); }
    }

    public int getSiguiente(int bloque) {
        bloquear();
        try { return ocupante[bloque] == null ? SIN_SIGUIENTE : siguiente[bloque]; } finally { mutex.release(); }
    }

    public boolean esBloqueSwap(int bloque) {
        bloquear();
        try { return esSwap[bloque]; } finally { mutex.release(); }
    }

    public int getTotalBloques() { return totalBloques; }
    public int getTamanoSwap() { return tamanoSwap; }

    public int getBloquesLibres() {
        bloquear();
        try { return bloquesLibres; } finally { mutex.release(); }
    }

    public int getBloquesSwapUsados() {
        bloquear();
        try { return bloquesSwapUsados; } finally { mutex.release(); }
    }

    /** Bloques que todavía pueden usar los archivos (los libres menos lo que falta del cupo de swap). */
    public int getBloquesDisponiblesParaArchivos() {
        bloquear();
        try { return Math.max(0, bloquesLibres - (tamanoSwap - bloquesSwapUsados)); } finally { mutex.release(); }
    }

    public PlanificadorDisco getPlanificador() {
        return planificador;
    }

    // ===================== Auxiliares =====================

    /**
     * Toma "cantidad" bloques libres y los encadena. Si primerBloque >= 0, la cadena
     * empieza ahí; los demás se buscan desde el bloque 0. Se llama con el mutex tomado
     * y después de verificar que hay espacio.
     */
    private int encadenar(int primerBloque, String quien, int cantidad, boolean swap) {
        int primero = primerBloque >= 0 ? primerBloque : buscarLibre(0);
        int actual = primero;
        ocupar(actual, quien, swap);
        int busqueda = primero + 1; // los siguientes se buscan después del primero (y luego desde el 0)
        for (int i = 1; i < cantidad; i++) {
            int nuevo = buscarLibre(busqueda);
            ocupar(nuevo, quien, swap);
            siguiente[actual] = nuevo;
            actual = nuevo;
            busqueda = nuevo + 1;
        }
        siguiente[actual] = SIN_SIGUIENTE;
        return primero;
    }

    private void ocupar(int bloque, String quien, boolean swap) {
        ocupante[bloque] = quien;
        esSwap[bloque] = swap;
        siguiente[bloque] = SIN_SIGUIENTE;
        bloquesLibres--;
    }

    private int buscarLibre(int desde) {
        if (desde >= totalBloques) {
            desde = 0;
        }
        for (int i = desde; i < totalBloques; i++) {
            if (ocupante[i] == null) {
                return i;
            }
        }
        for (int i = 0; i < desde; i++) {
            if (ocupante[i] == null) {
                return i;
            }
        }
        throw new IllegalStateException("No quedan bloques libres en el disco");
    }

    private void validarCantidad(int cantidad) {
        if (cantidad < 1) {
            throw new IllegalArgumentException("Hay que asignar al menos un bloque");
        }
    }

    private void validarInicioDeCadena(int primerBloque) {
        if (primerBloque < 0 || primerBloque >= totalBloques || ocupante[primerBloque] == null) {
            throw new IllegalArgumentException("El bloque " + primerBloque + " no es el inicio de una cadena ocupada");
        }
    }

    private void bloquear() {
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido esperando el disco", e);
        }
    }
}
