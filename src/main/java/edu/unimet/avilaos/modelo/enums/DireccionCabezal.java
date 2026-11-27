package edu.unimet.avilaos.modelo.enums;

/** Hacia dónde se mueve el cabezal del disco. */
public enum DireccionCabezal {
    /** Hacia los bloques mayores. */
    ARRIBA,
    /** Hacia los bloques menores. */
    ABAJO;

    public DireccionCabezal opuesta() {
        return this == ARRIBA ? ABAJO : ARRIBA;
    }
}
