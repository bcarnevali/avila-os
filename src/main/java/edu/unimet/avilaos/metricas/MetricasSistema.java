package edu.unimet.avilaos.metricas;

/**
 * Metricas del sistema distribuido completo. No cuenta nada por su cuenta:
 * suma los contadores de cada computador y calcula los promedios sobre el total
 * (por ejemplo, la equidad de Jain se calcula con las esperas de todos los procesos).
 */
public class MetricasSistema implements FuenteMetricas {

    private long ciclos;
    private long ciclosTotales;
    private long ciclosCpuOcupada;
    private int terminados;
    private int vencidos;
    private long sumaRespuesta;
    private int cantidadRespuestas;
    private double sumaEspera;
    private double sumaEsperaCuadrados;
    private int cantidadEsperas;
    private long sumaBloqueo;
    private int cantidadBloqueos;
    private int producidos;
    private int consumidos;

    public MetricasSistema(Metricas[] porComputador) {
        for (Metricas m : porComputador) {
            ciclos = Math.max(ciclos, m.getCiclosTotales());
            ciclosTotales += m.getCiclosTotales();
            ciclosCpuOcupada += m.getCiclosCpuOcupada();
            terminados += m.getProcesosTerminados();
            vencidos += m.getDeadlinesVencidos();
            sumaRespuesta += m.getSumaRespuesta();
            cantidadRespuestas += m.getCantidadRespuestas();
            sumaEspera += m.getSumaEspera();
            sumaEsperaCuadrados += m.getSumaEsperaCuadrados();
            cantidadEsperas += m.getCantidadEsperas();
            sumaBloqueo += m.getSumaBloqueoSemaforo();
            cantidadBloqueos += m.getCantidadBloqueosSemaforo();
            producidos += m.getElementosProducidos();
            consumidos += m.getElementosConsumidos();
        }
    }

    /** Procesos terminados en todo el sistema por ciclo de reloj. */
    @Override
    public double getThroughput() {
        return ciclos == 0 ? 0 : (double) terminados / ciclos;
    }

    /** Porcentaje de ciclos ocupados sobre los ciclos de todas las CPU juntas. */
    @Override
    public double getUtilizacionCpu() {
        return ciclosTotales == 0 ? 0 : (ciclosCpuOcupada * 100.0) / ciclosTotales;
    }

    @Override
    public double getTiempoRespuestaPromedio() {
        return cantidadRespuestas == 0 ? 0 : (double) sumaRespuesta / cantidadRespuestas;
    }

    @Override
    public double getPorcentajeDeadlinesCumplidos() {
        int total = terminados + vencidos;
        return total == 0 ? 100 : (terminados * 100.0) / total;
    }

    @Override
    public double getIndiceEquidad() {
        if (cantidadEsperas == 0 || sumaEsperaCuadrados == 0) {
            return 1;
        }
        return (sumaEspera * sumaEspera) / (cantidadEsperas * sumaEsperaCuadrados);
    }

    @Override
    public int getElementosProducidos() {
        return producidos;
    }

    @Override
    public int getElementosConsumidos() {
        return consumidos;
    }

    @Override
    public double getBloqueoPromedioSemaforo() {
        return cantidadBloqueos == 0 ? 0 : (double) sumaBloqueo / cantidadBloqueos;
    }
}
