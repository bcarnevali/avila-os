package edu.unimet.avilaos.kernel;

/**
 * Recibe cada decisión importante del sistema operativo para el log de eventos.
 * La interfaz gráfica la implementa para mostrar los mensajes, así el núcleo
 * no depende de ninguna ventana.
 */
public interface RegistroEventos {
    void registrar(long ciclo, String mensaje);
}
