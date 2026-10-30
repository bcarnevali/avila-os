package edu.unimet.avilaos.metricas;

/**
 * Lo que la interfaz grafica necesita leer de unas metricas. La implementan
 * las metricas de un computador (Metricas) y las del sistema completo
 * (MetricasSistema), asi la tabla las muestra igual sin saber de donde vienen.
 */
public interface FuenteMetricas {

    double getThroughput();

    double getUtilizacionCpu();

    double getTiempoRespuestaPromedio();

    double getPorcentajeDeadlinesCumplidos();

    double getIndiceEquidad();

    int getElementosProducidos();

    int getElementosConsumidos();

    double getBloqueoPromedioSemaforo();
}
