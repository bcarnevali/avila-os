package edu.unimet.avilaos.kernel;

import edu.unimet.avilaos.cluster.Cluster;
import edu.unimet.avilaos.cluster.Computador;
import edu.unimet.avilaos.disco.Disco;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import edu.unimet.avilaos.modelo.enums.TipoProceso;

public class PruebaSuspension {

    public static void main(String[] args) {
        RegistroEventos log = (ciclo, msg) -> System.out.println("[Ciclo " + ciclo + "] " + msg);

        // RAM 40 y procesos de 16: caben 2 en RAM (32) y el 3ro queda en NUEVO -> dispara SWAP_OUT y luego SWAP_IN
        Cluster cluster = new Cluster(new int[]{40}, TipoPolitica.ROUND_ROBIN, 3, 50, log);
        Disco disco = new Disco(200, 100, 0, DireccionCabezal.ARRIBA, 25, TipoPoliticaDisco.FIFO, log);
        cluster.instalarDisco(1, disco, null);

        Computador comp = cluster.getComputador(1);

        PCB p1 = new PCB("P1", TipoProceso.IO_BOUND, 1, 16, 6, 500, 1, 0);
        p1.configurarES(2, 6);

        PCB p2 = new PCB("P2", TipoProceso.CPU_BOUND, 2, 16, 6, 500, 1, 0);
        PCB p3 = new PCB("P3", TipoProceso.CPU_BOUND, 1, 16, 6, 500, 1, 0);

        comp.crearProceso(p1, 0);
        comp.crearProceso(p2, 0);
        comp.crearProceso(p3, 0);

        boolean termino = false;
        for (long ciclo = 1; ciclo <= 200; ciclo++) {
            disco.tick(ciclo);
            comp.tick(ciclo);

            if (comp.getCpu().getEnEjecucion() == null
                    && !comp.getPlanificador().hayProcesosPendientes()
                    && disco.getPlanificador().estaOcioso()) {
                termino = true;
                break;
            }
        }

        if (!termino) {
            throw new AssertionError("La simulación no terminó en el límite de ciclos");
        }
        if (comp.getMedianoPlazo().getSuspensiones() < 1) {
            throw new AssertionError("Se esperaba al menos una suspensión (SWAP_OUT)");
        }
        if (comp.getMedianoPlazo().getReactivaciones() < 1) {
            throw new AssertionError("Se esperaba al menos una reactivación (SWAP_IN)");
        }
        if (comp.getPlanificador().getTerminados().length != 3) {
            throw new AssertionError("No terminaron los 3 procesos");
        }

        System.out.println("Suspensiones: " + comp.getMedianoPlazo().getSuspensiones()
                + " | Reactivaciones: " + comp.getMedianoPlazo().getReactivaciones());
        System.out.println("TODAS LAS PRUEBAS PASARON");
    }
}