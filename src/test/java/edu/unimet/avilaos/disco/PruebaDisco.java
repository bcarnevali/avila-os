package edu.unimet.avilaos.disco;

import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.OperacionES;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;

/**
 * Prueba del disco: asignación encadenada, área de intercambio y conexión con el
 * planificador. En NetBeans: clic derecho sobre este archivo → Run File.
 */
public class PruebaDisco {

    private static int fallos = 0;

    public static void main(String[] args) {
        // Disco de 200 bloques con 100 de swap, como el JSON del enunciado
        Disco disco = new Disco(200, 100, 50, DireccionCabezal.ARRIBA, 5, TipoPoliticaDisco.SSTF, null);

        System.out.println("=== Asignación encadenada ===");
        int notas = disco.asignarArchivoEn(34, "/usuarios/notas.txt", 3);
        verificar("empieza en el bloque pedido", notas == 34);
        verificar("la cadena es 34 → 35 → 36", texto(disco.getCadena(34)).equals("34 → 35 → 36"));
        int boot = disco.asignarArchivoEn(11, "/sistema/boot.bin", 2);
        verificar("boot.bin: 11 → 12", texto(disco.getCadena(boot)).equals("11 → 12"));
        int video = disco.asignarArchivo("/usuarios/video.mp4", 4);
        verificar("archivo nuevo sin bloque fijo toma los primeros libres (0 → 1 → 2 → 3)",
                texto(disco.getCadena(video)).equals("0 → 1 → 2 → 3"));
        verificar("ocupante del bloque 35 es notas.txt", "/usuarios/notas.txt".equals(disco.getOcupante(35)));
        verificar("el último bloque no tiene siguiente", disco.getSiguiente(36) == Disco.SIN_SIGUIENTE);
        verificar("libres = 200 - 9 = 191", disco.getBloquesLibres() == 191);
        verificar("para archivos quedan 191 - 100 de swap = 91", disco.getBloquesDisponiblesParaArchivos() == 91);

        disco.agregarBloques(34, 2);
        verificar("notas.txt crece: 34 → 35 → 36 → 4 → 5", texto(disco.getCadena(34)).equals("34 → 35 → 36 → 4 → 5"));

        disco.renombrarOcupante(34, "/usuarios/apuntes.txt");
        verificar("al renombrar cambia el ocupante de toda la cadena", "/usuarios/apuntes.txt".equals(disco.getOcupante(5)));

        boolean rechazado = false;
        try {
            disco.asignarArchivoEn(35, "/x.txt", 1);
        } catch (IllegalStateException e) {
            rechazado = true;
        }
        verificar("no deja empezar un archivo en un bloque ocupado", rechazado);

        rechazado = false;
        try {
            disco.asignarArchivo("/grande.iso", 92);
        } catch (IllegalStateException e) {
            rechazado = true;
        }
        verificar("un archivo no puede usar el cupo del swap (92 > 89 disponibles)", rechazado);

        System.out.println();
        System.out.println("=== Área de intercambio (swap) ===");
        verificar("caben 16 bloques en swap", disco.cabeEnSwap(16));
        int swapP1 = disco.asignarSwap("P1 (suspendido)", 16);
        verificar("swap usado = 16", disco.getBloquesSwapUsados() == 16);
        verificar("el bloque " + swapP1 + " está marcado como swap", disco.esBloqueSwap(swapP1));
        verificar("la cadena del swap tiene 16 bloques", disco.getCadena(swapP1).length == 16);
        verificar("para archivos siguen quedando 89", disco.getBloquesDisponiblesParaArchivos() == 89);
        int swapP2 = disco.asignarSwap("P2 (suspendido)", 80);
        verificar("no caben 16 más en swap (96 + 16 > 100)", !disco.cabeEnSwap(16));
        rechazado = false;
        try {
            disco.asignarSwap("P3 (suspendido)", 16);
        } catch (IllegalStateException e) {
            rechazado = true;
        }
        verificar("swap lleno: rechaza la suspensión", rechazado);
        disco.liberar(swapP1);
        verificar("al reactivar P1 se liberan sus 16 bloques de swap", disco.getBloquesSwapUsados() == 80);
        disco.liberar(swapP2);
        disco.liberar(video);
        verificar("al liberar todo vuelven los bloques", disco.getBloquesLibres() == 200 - 7);

        System.out.println();
        System.out.println("=== Disco conectado al reloj ===");
        final int[] terminadas = {0};
        final long[] ultimoCiclo = {0};
        disco.setReceptor((s, ciclo) -> {
            terminadas[0]++;
            ultimoCiclo[0] = ciclo;
        });
        disco.getPlanificador().encolar(new SolicitudES("P1", OperacionES.LEER, 34, 5, 0), 0);
        disco.getPlanificador().encolar(new SolicitudES("P2", OperacionES.LEER, 11, 2, 0), 0);
        for (long ciclo = 1; ciclo <= 100; ciclo++) {
            disco.tick(ciclo);
        }
        verificar("terminaron las 2 solicitudes", terminadas[0] == 2);
        // 50→34: 16 posiciones a 5 por ciclo = 4 ciclos, 5 bloques = 5 ciclos;
        // 34→11: 23 posiciones = 5 ciclos, 2 bloques = 2 ciclos. Total 16 ciclos.
        verificar("la última termina en el ciclo 16 (fue " + ultimoCiclo[0] + ")", ultimoCiclo[0] == 16);
        verificar("desplazamiento 16 + 23 = 39", disco.getPlanificador().getDesplazamientoTotal() == 39);

        System.out.println();
        System.out.println(fallos == 0 ? "TODAS LAS PRUEBAS PASARON" : fallos + " PRUEBA(S) FALLARON");
    }

    private static void verificar(String descripcion, boolean ok) {
        if (!ok) {
            fallos++;
        }
        System.out.println((ok ? "OK     " : "FALLA  ") + descripcion);
    }

    private static String texto(int[] v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            sb.append(i == 0 ? "" : " → ").append(v[i]);
        }
        return sb.toString();
    }
}
