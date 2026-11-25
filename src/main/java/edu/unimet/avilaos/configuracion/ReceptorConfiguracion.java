package edu.unimet.avilaos.configuracion;

import java.io.File;
import java.io.IOException;

/** Contrato entre la vista y el controlador para cargar ambos JSON. */
public interface ReceptorConfiguracion {
    void aplicarConfiguracion(File procesos, File archivos) throws IOException;
    void guardarSistemaArchivos(File destino) throws IOException;
}
