package edu.unimet.avilaos.configuracion;

/**
 * La vista de configuracion solo arma una ConfiguracionSimulacion y la entrega.
 * Quien la implemente (el controlador) reinicia el cluster con ella.
 */
public interface ReceptorConfiguracion {

    /** Reinicia la simulacion con esta configuracion. */
    void aplicarConfiguracion(ConfiguracionSimulacion configuracion);

    /** Configuracion con la que esta corriendo la simulacion actual. */
    ConfiguracionSimulacion getConfiguracionActual();

    /** Procesos y buffers creados en la simulacion actual, como carga inicial para guardar. */
    ConfiguracionSimulacion capturarCargaActual();
}
