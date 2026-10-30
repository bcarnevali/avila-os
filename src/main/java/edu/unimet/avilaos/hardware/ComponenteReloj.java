package edu.unimet.avilaos.hardware;

/** Todo componente que avanza con el reloj global (CPU, computador, dispositivo E/S). */
public interface ComponenteReloj {
    void tick(long cicloActual);
}
