package edu.unimet.avilaos.configuracion;

import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.OperacionES;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import edu.unimet.avilaos.modelo.enums.TipoProceso;
import java.io.File;

/**
 * Prueba de la lectura del JSON de procesos con el ejemplo del enunciado y con
 * archivos inválidos. En NetBeans: clic derecho sobre este archivo → Run File.
 */
public class PruebaJsonProcesos {

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Ejemplo del enunciado (ejemplos/procesos_enunciado.json) ===");
        ConfiguracionProcesos c = GestorJsonProcesos.cargar(new File("ejemplos/procesos_enunciado.json"));
        verificar("duración del ciclo 300 ms", c.getDuracionCicloMs() == 300);
        verificar("swap de 100 bloques", c.getTamanoSwap() == 100);
        verificar("política de disco FIFO", c.getPoliticaDisco() == TipoPoliticaDisco.FIFO);
        verificar("RAM 1024", c.getRam() == 1024);
        verificar("política de CPU FCFS", c.getPolitica() == TipoPolitica.FCFS);
        verificar("quantum 0 con FCFS queda en el valor por defecto", c.getQuantum() == GestorJsonProcesos.QUANTUM_POR_DEFECTO);
        verificar("8 procesos", c.getProcesos().tamano() == 8);
        ProcesoJson p3 = c.getProcesos().obtener(2);
        verificar("el 3.º es Proc_34 UPDATE /usuarios/notas.txt", p3.getNombre().equals("Proc_34")
                && p3.getOperacion() == OperacionES.ACTUALIZAR && p3.getRuta().equals("/usuarios/notas.txt"));
        PCB pcb = p3.crearPCB(1, 0);
        verificar("crea un PCB de tipo ES_ARCHIVO con memoria 16 y 2 instrucciones",
                pcb.getTipo() == TipoProceso.ES_ARCHIVO && pcb.getMemoriaRequerida() == 16
                && pcb.getInstruccionesTotales() == 2 && pcb.getOperacionArchivo() == OperacionES.ACTUALIZAR);

        System.out.println();
        System.out.println("=== Demo de suspensión (ejemplos/procesos_suspension.json) ===");
        ConfiguracionProcesos s = GestorJsonProcesos.cargar(new File("ejemplos/procesos_suspension.json"));
        verificar("RAM 40 (caben 2 procesos de 16)", s.getRam() == 40);
        verificar("9 procesos, el último es un CREATE de 3 bloques",
                s.getProcesos().tamano() == 9 && s.getProcesos().obtener(8).getOperacion() == OperacionES.CREAR
                && s.getProcesos().obtener(8).getBloques() == 3);

        System.out.println();
        System.out.println("=== Archivos inválidos ===");
        String base = "{\"globalConfig\":{\"cycleDurationMs\":300,\"swapSizeBlocks\":100,\"initialDiskPolicy\":\"%s\"},"
                + "\"computer\":{\"ramCapacity\":%s,\"policy\":\"%s\",\"quantum\":%s},"
                + "\"processes\":[{\"name\":\"P\",\"memory\":%s,\"deadline\":500,\"instructions\":2,"
                + "\"targetPath\":\"%s\",\"operation\":\"%s\",\"fileSizeBlocks\":%s}]}";
        rechaza("texto que no es JSON", "esto no es json {");
        rechaza("letras en la RAM", String.format(base, "FIFO", "\"mil\"", "FCFS", "0", "16", "/a.txt", "READ", "0"));
        rechaza("RAM fuera de rango", String.format(base, "FIFO", "5", "FCFS", "0", "4", "/a.txt", "READ", "0"));
        rechaza("política de disco desconocida", String.format(base, "LOOKX", "1024", "FCFS", "0", "16", "/a.txt", "READ", "0"));
        rechaza("Round Robin con quantum 0", String.format(base, "FIFO", "1024", "RR", "0", "16", "/a.txt", "READ", "0"));
        rechaza("proceso más grande que la RAM", String.format(base, "FIFO", "64", "FCFS", "0", "100", "/a.txt", "READ", "0"));
        rechaza("ruta sin /", String.format(base, "FIFO", "1024", "FCFS", "0", "16", "a.txt", "READ", "0"));
        rechaza("operación desconocida", String.format(base, "FIFO", "1024", "FCFS", "0", "16", "/a.txt", "COPY", "0"));
        rechaza("CREATE sin bloques", String.format(base, "FIFO", "1024", "FCFS", "0", "16", "/a.txt", "CREATE", "0"));
        rechaza("falta processes", "{\"globalConfig\":{\"cycleDurationMs\":300,\"swapSizeBlocks\":100,"
                + "\"initialDiskPolicy\":\"FIFO\"},\"computer\":{\"ramCapacity\":1024,\"policy\":\"FCFS\",\"quantum\":0}}");
        ConfiguracionProcesos cs = GestorJsonProcesos.leer(String.format(base, "c-scan", "1024", "Round_Robin", "4", "16", "/a.txt", "create", "2"));
        verificar("acepta \"c-scan\", \"Round_Robin\" y \"create\" en minúsculas",
                cs.getPoliticaDisco() == TipoPoliticaDisco.C_SCAN && cs.getPolitica() == TipoPolitica.ROUND_ROBIN
                && cs.getQuantum() == 4 && cs.getProcesos().obtener(0).getOperacion() == OperacionES.CREAR);

        System.out.println();
        System.out.println(fallos == 0 ? "TODAS LAS PRUEBAS PASARON" : fallos + " PRUEBA(S) FALLARON");
    }

    private static void rechaza(String caso, String json) {
        try {
            GestorJsonProcesos.leer(json);
            verificar("rechaza " + caso, false);
        } catch (IllegalArgumentException e) {
            verificar("rechaza " + caso + " → " + e.getMessage(), true);
        }
    }

    private static void verificar(String descripcion, boolean ok) {
        if (!ok) {
            fallos++;
        }
        System.out.println((ok ? "OK     " : "FALLA  ") + descripcion);
    }
}
