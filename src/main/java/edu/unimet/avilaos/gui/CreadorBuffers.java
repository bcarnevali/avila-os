package edu.unimet.avilaos.gui;

/** La GUI pide crear un buffer; quien la implemente decide si se puede y lo crea. */
public interface CreadorBuffers {

    void crearBuffer(String id, int capacidad, int idAnfitrion);
}
