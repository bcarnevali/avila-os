package edu.unimet.avilaos.modelo.enums;

/** Operaciones que pasan por la cola de E/S del disco. */
public enum OperacionES {
    CREAR, LEER, ACTUALIZAR, ELIMINAR,
    /** Escribir en swap la imagen de un proceso que se suspende. */
    SWAP_OUT,
    /** Leer de swap la imagen de un proceso que se reactiva. */
    SWAP_IN
}
