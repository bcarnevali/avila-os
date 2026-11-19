package edu.unimet.avilaos.modelo.enums;

public enum TipoProceso {
    CPU_BOUND, IO_BOUND, PRODUCTOR, CONSUMIDOR,
    /** Proceso de E/S que hace una operación (crear, leer, actualizar o eliminar) sobre un archivo del disco. */
    ES_ARCHIVO
}
