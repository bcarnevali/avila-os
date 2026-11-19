package edu.unimet.avilaos.cluster;

import edu.unimet.avilaos.disco.Disco;
import edu.unimet.avilaos.disco.SolicitudES;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.kernel.ServicioArchivos;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.EstadoProceso;
import edu.unimet.avilaos.modelo.enums.OperacionES;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import edu.unimet.avilaos.modelo.enums.TipoProceso;

/**
 * Prueba de los procesos de E/S conectados al disco, con los 8 procesos del JSON
 * del enunciado. El sistema de archivos se simula con una tabla fija de rutas.
 * En NetBeans: clic derecho sobre este archivo → Run File.
 */
public class PruebaProcesosES {

    private static int fallos = 0;

    /** Archivos del JSON del enunciado: ruta y primer bloque. */
    private static final String[] RUTAS = {
        "/sistema/boot.bin", "/usuarios/notas.txt", "/sistema/script.py", "/sistema/core.dll",
        "/sistema/config.sys", "/usuarios/img1.png", "/usuarios/data.csv", "/usuarios/video.mp4"};
    private static final int[] BLOQUES = {11, 34, 62, 64, 95, 119, 123, 180};

    /** Procesos del JSON del enunciado, en orden. */
    private static final String[] PROC_NOMBRE = {
        "Proc_95", "Proc_180", "Proc_34", "Proc_119", "Proc_11", "Proc_123", "Proc_62", "Proc_64"};
    private static final String[] PROC_RUTA = {
        "/sistema/config.sys", "/usuarios/video.mp4", "/usuarios/notas.txt", "/usuarios/img1.png",
        "/sistema/boot.bin", "/usuarios/data.csv", "/sistema/script.py", "/sistema/core.dll"};
    private static final OperacionES[] PROC_OP = {
        OperacionES.LEER, OperacionES.LEER, OperacionES.ACTUALIZAR, OperacionES.ELIMINAR,
        OperacionES.LEER, OperacionES.ACTUALIZAR, OperacionES.LEER, OperacionES.LEER};

    public static void main(String[] args) {
        for (TipoPoliticaDisco politica : TipoPoliticaDisco.values()) {
            probar(politica);
        }
        probarRechazo();
        System.out.println();
        System.out.println(fallos == 0 ? "TODAS LAS PRUEBAS PASARON" : fallos + " PRUEBA(S) FALLARON");
    }

    private static void probar(TipoPoliticaDisco politica) {
        System.out.println("=== 8 procesos de E/S del JSON con " + politica + " ===");
        Cluster cluster = new Cluster(new int[] {1024}, TipoPolitica.FCFS, 3, 100, null);
        Disco disco = new Disco(200, 100, 50, DireccionCabezal.ARRIBA, 5, politica, null);
        ServicioDePrueba servicio = new ServicioDePrueba();
        cluster.instalarDisco(1, disco, servicio);
        Computador computador = cluster.getComputador(1);

        PCB[] procesos = new PCB[PROC_NOMBRE.length];
        for (int i = 0; i < procesos.length; i++) {
            procesos[i] = new PCB(PROC_NOMBRE[i], TipoProceso.ES_ARCHIVO, 1, 16, 2, 500, 1, 0);
            procesos[i].configurarArchivo(PROC_OP[i], PROC_RUTA[i], 0);
            computador.crearProceso(procesos[i], 0);
        }

        // Se avanza a mano igual que el reloj: primero el computador, luego el disco
        long ciclo = 1;
        for (; ciclo < 2000 && terminados(procesos) < procesos.length; ciclo++) {
            computador.tick(ciclo);
            disco.tick(ciclo);
        }
        verificar("los 8 procesos terminan (ciclo " + ciclo + ")", terminados(procesos) == procesos.length);
        verificar("el disco atendió 8 solicitudes", disco.getPlanificador().getAtendidas() == 8);
        verificar("el archivo de Proc_119 (DELETE) se eliminó", servicio.eliminados == 1);
        System.out.println("       orden de atención: " + servicio.orden
                + "  desplazamiento " + disco.getPlanificador().getDesplazamientoTotal());
        if (politica == TipoPoliticaDisco.FIFO) {
            verificar("FIFO atiende en orden de llegada (95 → 180 → 34 → 119 → 11 → 123 → 62 → 64)",
                    servicio.orden.toString().equals("95 180 34 119 11 123 62 64 "));
        }
        System.out.println();
    }

    private static void probarRechazo() {
        System.out.println("=== Operación sobre un archivo que no existe ===");
        Cluster cluster = new Cluster(new int[] {1024}, TipoPolitica.FCFS, 3, 100, null);
        Disco disco = new Disco(200, 100, 50, DireccionCabezal.ARRIBA, 5, TipoPoliticaDisco.FIFO, null);
        cluster.instalarDisco(1, disco, new ServicioDePrueba());
        Computador computador = cluster.getComputador(1);
        PCB p = new PCB("Fantasma", TipoProceso.ES_ARCHIVO, 1, 16, 2, 500, 1, 0);
        p.configurarArchivo(OperacionES.LEER, "/no/existe.txt", 0);
        computador.crearProceso(p, 0);
        for (long ciclo = 1; ciclo < 20; ciclo++) {
            computador.tick(ciclo);
            disco.tick(ciclo);
        }
        verificar("el proceso termina sin bloquearse", p.getEstado() == EstadoProceso.TERMINADO);
        verificar("no llega nada al disco", disco.getPlanificador().getAtendidas() == 0);
    }

    private static int terminados(PCB[] procesos) {
        int n = 0;
        for (PCB p : procesos) {
            if (p.getEstado() == EstadoProceso.TERMINADO) {
                n++;
            }
        }
        return n;
    }

    private static void verificar(String descripcion, boolean ok) {
        if (!ok) {
            fallos++;
        }
        System.out.println((ok ? "OK     " : "FALLA  ") + descripcion);
    }

    /** Sistema de archivos falso: solo conoce las rutas del JSON del enunciado. */
    private static class ServicioDePrueba implements ServicioArchivos {
        final StringBuilder orden = new StringBuilder();
        int eliminados = 0;

        @Override
        public SolicitudES prepararSolicitud(PCB proceso, long ciclo) {
            for (int i = 0; i < RUTAS.length; i++) {
                if (RUTAS[i].equals(proceso.getRutaArchivo())) {
                    return new SolicitudES(proceso, proceso.getOperacionArchivo(), BLOQUES[i], 1, ciclo);
                }
            }
            throw new IllegalStateException("el archivo no existe");
        }

        @Override
        public void completarOperacion(PCB proceso, SolicitudES solicitud, long ciclo) {
            orden.append(solicitud.getBloque()).append(' ');
            if (solicitud.getOperacion() == OperacionES.ELIMINAR) {
                eliminados++;
            }
        }
    }
}
