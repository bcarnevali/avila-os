package edu.unimet.avilaos.disco;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.OperacionES;

/**
 * Una solicitud en la cola de E/S del disco. Guarda quién la pidió, qué operación
 * es, en qué bloque está el cabezal de destino y cuántos bloques hay que leer o
 * escribir (cada bloque cuesta un ciclo).
 */
public class SolicitudES {

    private final PCB proceso;
    private final String solicitante;
    private final OperacionES operacion;
    private final int bloque;
    private final int bloquesATransferir;
    private final long cicloLlegada;
    private long cicloInicio = -1;
    private long cicloFin = -1;

    /** Solicitud de un proceso de ÁvilaOS. */
    public SolicitudES(PCB proceso, OperacionES operacion, int bloque, int bloquesATransferir, long cicloLlegada) {
        this(proceso, proceso != null ? proceso.toString() : "?", operacion, bloque, bloquesATransferir, cicloLlegada);
    }

    /** Solicitud sin PCB (se usa para probar el disco aislado). */
    public SolicitudES(String solicitante, OperacionES operacion, int bloque, int bloquesATransferir, long cicloLlegada) {
        this(null, solicitante, operacion, bloque, bloquesATransferir, cicloLlegada);
    }

    private SolicitudES(PCB proceso, String solicitante, OperacionES operacion, int bloque,
                        int bloquesATransferir, long cicloLlegada) {
        if (operacion == null) {
            throw new IllegalArgumentException("La operación es obligatoria");
        }
        if (bloque < 0) {
            throw new IllegalArgumentException("El bloque no puede ser negativo");
        }
        if (bloquesATransferir < 1) {
            throw new IllegalArgumentException("Hay que transferir al menos un bloque");
        }
        this.proceso = proceso;
        this.solicitante = solicitante;
        this.operacion = operacion;
        this.bloque = bloque;
        this.bloquesATransferir = bloquesATransferir;
        this.cicloLlegada = cicloLlegada;
    }

    public PCB getProceso() { return proceso; }
    public String getSolicitante() { return solicitante; }
    public OperacionES getOperacion() { return operacion; }
    public int getBloque() { return bloque; }
    public int getBloquesATransferir() { return bloquesATransferir; }
    public long getCicloLlegada() { return cicloLlegada; }
    public long getCicloInicio() { return cicloInicio; }
    public long getCicloFin() { return cicloFin; }

    void setCicloInicio(long ciclo) { this.cicloInicio = ciclo; }
    void setCicloFin(long ciclo) { this.cicloFin = ciclo; }

    /** Ciclos que esperó en la cola antes de que el disco la eligiera. */
    public long getEspera() {
        return cicloInicio < 0 ? -1 : cicloInicio - cicloLlegada;
    }

    @Override
    public String toString() {
        return solicitante + " " + operacion + " bloque " + bloque;
    }
}
