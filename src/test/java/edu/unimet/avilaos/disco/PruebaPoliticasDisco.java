package edu.unimet.avilaos.disco;

import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.OperacionES;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;

/**
 * Prueba del disco aislado con los casos P1–P4 y el ejemplo del anexo del enunciado.
 * En NetBeans: clic derecho sobre este archivo → Run File.
 */
public class PruebaPoliticasDisco {

    private static int fallos = 0;

    public static void main(String[] args) {
        int[] casos = {95, 180, 34, 119, 11, 123, 62, 64};
        System.out.println("=== Casos de prueba P1–P4 (cabezal en 50, hacia los bloques mayores) ===");
        probar("P1 FIFO", TipoPoliticaDisco.FIFO, casos, new int[] {95, 180, 34, 119, 11, 123, 62, 64}, -1);
        probar("P2 SSTF", TipoPoliticaDisco.SSTF, casos, new int[] {62, 64, 34, 11, 95, 119, 123, 180}, -1);
        probar("P3 SCAN", TipoPoliticaDisco.SCAN, casos, new int[] {62, 64, 95, 119, 123, 180, 34, 11}, -1);
        probar("P4 C-SCAN", TipoPoliticaDisco.C_SCAN, casos, new int[] {62, 64, 95, 119, 123, 180, 11, 34}, -1);

        int[] anexo = {82, 17, 43, 140, 24, 16, 190};
        System.out.println();
        System.out.println("=== Anexo (cabezal en 50, hacia los bloques mayores) ===");
        probar("FIFO", TipoPoliticaDisco.FIFO, anexo, new int[] {82, 17, 43, 140, 24, 16, 190}, 518);
        probar("SSTF", TipoPoliticaDisco.SSTF, anexo, new int[] {43, 24, 17, 16, 82, 140, 190}, 208);
        probar("SCAN", TipoPoliticaDisco.SCAN, anexo, new int[] {82, 140, 190, 43, 24, 17, 16}, 332);
        probar("C-SCAN", TipoPoliticaDisco.C_SCAN, anexo, new int[] {82, 140, 190, 16, 17, 24, 43}, 192);

        System.out.println();
        System.out.println(fallos == 0 ? "TODAS LAS PRUEBAS PASARON" : fallos + " PRUEBA(S) FALLARON");
    }

    /** Encola todo en el ciclo 0 y avanza el disco hasta vaciar la cola. */
    private static void probar(String nombre, TipoPoliticaDisco politica, int[] bloques,
                               int[] ordenEsperado, long desplazamientoEsperado) {
        PlanificadorDisco disco = new PlanificadorDisco(200, 50, DireccionCabezal.ARRIBA, 3, politica, null);
        for (int i = 0; i < bloques.length; i++) {
            disco.encolar(new SolicitudES("S" + (i + 1), OperacionES.LEER, bloques[i], 1, 0), 0);
        }
        int[] orden = new int[bloques.length];
        int atendidas = 0;
        for (long ciclo = 1; ciclo < 10_000 && atendidas < bloques.length; ciclo++) {
            SolicitudES terminada = disco.avanzar(ciclo);
            if (terminada != null) {
                orden[atendidas++] = terminada.getBloque();
            }
        }
        boolean ordenOk = iguales(orden, ordenEsperado);
        boolean despOk = desplazamientoEsperado < 0 || disco.getDesplazamientoTotal() == desplazamientoEsperado;
        if (!ordenOk || !despOk) {
            fallos++;
        }
        System.out.printf("%-10s %s  orden %s  desplazamiento %d%s%n", nombre,
                ordenOk && despOk ? "OK   " : "FALLA", texto(orden), disco.getDesplazamientoTotal(),
                desplazamientoEsperado < 0 ? "" : " (esperado " + desplazamientoEsperado + ")");
    }

    private static boolean iguales(int[] a, int[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static String texto(int[] v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            sb.append(i == 0 ? "" : " → ").append(v[i]);
        }
        return sb.toString();
    }
}
