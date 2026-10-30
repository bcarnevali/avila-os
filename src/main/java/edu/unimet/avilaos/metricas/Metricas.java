package edu.unimet.avilaos.metricas;

import java.util.concurrent.Semaphore;

/**
 * Métricas de desempeño. Se usa una instancia por computador.
 * Solo lleva contadores y sumas, así que no necesita colecciones.
 * Protegida con Semaphore de Java porque la escribe el hilo del reloj
 * y la lee el hilo de la GUI.
 */
public class Metricas implements FuenteMetricas {

    private final Semaphore mutex = new Semaphore(1);

    private long ciclosTotales;
    private long ciclosCpuOcupada;
    private int procesosTerminados;
    private int deadlinesVencidos;
    private long sumaRespuesta;
    private int cantidadRespuestas;
    private double sumaEspera;
    private double sumaEsperaCuadrados;
    private int cantidadEsperas;
    private long sumaBloqueoSemaforo;
    private int cantidadBloqueosSemaforo;
    private int elementosProducidos;
    private int elementosConsumidos;

    /** Se llama una vez por ciclo de reloj. */
    public void registrarCiclo(boolean cpuOcupada) {
        mutex.acquireUninterruptibly();
        try {
            ciclosTotales++;
            if (cpuOcupada) {
                ciclosCpuOcupada++;
            }
        } finally {
            mutex.release();
        }
    }

    /** Un proceso terminó todas sus instrucciones antes de su deadline. */
    public void registrarProcesoTerminado() {
        mutex.acquireUninterruptibly();
        try {
            procesosTerminados++;
        } finally {
            mutex.release();
        }
    }

    /** Un proceso fue terminado porque su deadline llegó a 0. */
    public void registrarDeadlineVencido() {
        mutex.acquireUninterruptibly();
        try {
            deadlinesVencidos++;
        } finally {
            mutex.release();
        }
    }

    /** Ciclos entre que el proceso llegó y su primera ejecución en CPU. */
    public void registrarTiempoRespuesta(long ciclos) {
        mutex.acquireUninterruptibly();
        try {
            sumaRespuesta += ciclos;
            cantidadRespuestas++;
        } finally {
            mutex.release();
        }
    }

    /** Ciclos totales que un proceso pasó en la cola de listos. Se usa para la equidad. */
    public void registrarTiempoEspera(long ciclos) {
        mutex.acquireUninterruptibly();
        try {
            sumaEspera += ciclos;
            sumaEsperaCuadrados += (double) ciclos * ciclos;
            cantidadEsperas++;
        } finally {
            mutex.release();
        }
    }

    /** Ciclos que un proceso estuvo bloqueado en un semáforo de ÁvilaOS. */
    public void registrarBloqueoSemaforo(long ciclos) {
        mutex.acquireUninterruptibly();
        try {
            sumaBloqueoSemaforo += ciclos;
            cantidadBloqueosSemaforo++;
        } finally {
            mutex.release();
        }
    }

    public void registrarProduccion() {
        mutex.acquireUninterruptibly();
        try {
            elementosProducidos++;
        } finally {
            mutex.release();
        }
    }

    public void registrarConsumo() {
        mutex.acquireUninterruptibly();
        try {
            elementosConsumidos++;
        } finally {
            mutex.release();
        }
    }

    /** Procesos terminados por ciclo. */
    public double getThroughput() {
        mutex.acquireUninterruptibly();
        try {
            return ciclosTotales == 0 ? 0 : (double) procesosTerminados / ciclosTotales;
        } finally {
            mutex.release();
        }
    }

    /** Porcentaje de ciclos con la CPU ocupada (0 a 100). */
    public double getUtilizacionCpu() {
        mutex.acquireUninterruptibly();
        try {
            return ciclosTotales == 0 ? 0 : (ciclosCpuOcupada * 100.0) / ciclosTotales;
        } finally {
            mutex.release();
        }
    }

    public double getTiempoRespuestaPromedio() {
        mutex.acquireUninterruptibly();
        try {
            return cantidadRespuestas == 0 ? 0 : (double) sumaRespuesta / cantidadRespuestas;
        } finally {
            mutex.release();
        }
    }

    /** Porcentaje de procesos que terminaron antes de su deadline (0 a 100). */
    public double getPorcentajeDeadlinesCumplidos() {
        mutex.acquireUninterruptibly();
        try {
            int total = procesosTerminados + deadlinesVencidos;
            return total == 0 ? 100 : (procesosTerminados * 100.0) / total;
        } finally {
            mutex.release();
        }
    }

    /**
     * Índice de equidad de Jain sobre los tiempos de espera:
     * (suma)^2 / (n * suma de cuadrados). Vale 1 si todos esperaron lo mismo
     * y se acerca a 1/n cuanto más desigual es el reparto.
     */
    public double getIndiceEquidad() {
        mutex.acquireUninterruptibly();
        try {
            if (cantidadEsperas == 0 || sumaEsperaCuadrados == 0) {
                return 1;
            }
            return (sumaEspera * sumaEspera) / (cantidadEsperas * sumaEsperaCuadrados);
        } finally {
            mutex.release();
        }
    }

    public double getBloqueoPromedioSemaforo() {
        mutex.acquireUninterruptibly();
        try {
            return cantidadBloqueosSemaforo == 0 ? 0
                    : (double) sumaBloqueoSemaforo / cantidadBloqueosSemaforo;
        } finally {
            mutex.release();
        }
    }

    public int getProcesosTerminados() {
        mutex.acquireUninterruptibly();
        try {
            return procesosTerminados;
        } finally {
            mutex.release();
        }
    }

    public int getDeadlinesVencidos() {
        mutex.acquireUninterruptibly();
        try {
            return deadlinesVencidos;
        } finally {
            mutex.release();
        }
    }

    public int getElementosProducidos() {
        mutex.acquireUninterruptibly();
        try {
            return elementosProducidos;
        } finally {
            mutex.release();
        }
    }

    public int getElementosConsumidos() {
        mutex.acquireUninterruptibly();
        try {
            return elementosConsumidos;
        } finally {
            mutex.release();
        }
    }

    public long getCiclosTotales() {
        mutex.acquireUninterruptibly();
        try {
            return ciclosTotales;
        } finally {
            mutex.release();
        }
    }

    /** Pone todo en cero, para iniciar una simulación nueva. */
    public void reiniciar() {
        mutex.acquireUninterruptibly();
        try {
            ciclosTotales = 0;
            ciclosCpuOcupada = 0;
            procesosTerminados = 0;
            deadlinesVencidos = 0;
            sumaRespuesta = 0;
            cantidadRespuestas = 0;
            sumaEspera = 0;
            sumaEsperaCuadrados = 0;
            cantidadEsperas = 0;
            sumaBloqueoSemaforo = 0;
            cantidadBloqueosSemaforo = 0;
            elementosProducidos = 0;
            elementosConsumidos = 0;
        } finally {
            mutex.release();
        }
    }
    // ---------- Contadores crudos, para calcular las metricas del sistema completo ----------

    public long getCiclosCpuOcupada() {
        mutex.acquireUninterruptibly();
        try {
            return ciclosCpuOcupada;
        } finally {
            mutex.release();
        }
    }

    public long getSumaRespuesta() {
        mutex.acquireUninterruptibly();
        try {
            return sumaRespuesta;
        } finally {
            mutex.release();
        }
    }

    public int getCantidadRespuestas() {
        mutex.acquireUninterruptibly();
        try {
            return cantidadRespuestas;
        } finally {
            mutex.release();
        }
    }

    public double getSumaEspera() {
        mutex.acquireUninterruptibly();
        try {
            return sumaEspera;
        } finally {
            mutex.release();
        }
    }

    public double getSumaEsperaCuadrados() {
        mutex.acquireUninterruptibly();
        try {
            return sumaEsperaCuadrados;
        } finally {
            mutex.release();
        }
    }

    public int getCantidadEsperas() {
        mutex.acquireUninterruptibly();
        try {
            return cantidadEsperas;
        } finally {
            mutex.release();
        }
    }

    public long getSumaBloqueoSemaforo() {
        mutex.acquireUninterruptibly();
        try {
            return sumaBloqueoSemaforo;
        } finally {
            mutex.release();
        }
    }

    public int getCantidadBloqueosSemaforo() {
        mutex.acquireUninterruptibly();
        try {
            return cantidadBloqueosSemaforo;
        } finally {
            mutex.release();
        }
    }
}
