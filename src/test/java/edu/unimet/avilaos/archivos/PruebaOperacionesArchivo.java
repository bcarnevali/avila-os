package edu.unimet.avilaos.archivos;

import edu.unimet.avilaos.archivos.GestorJsonSistemaArchivos.ResultadoCarga;
import edu.unimet.avilaos.cluster.Cluster;
import edu.unimet.avilaos.cluster.Computador;
import edu.unimet.avilaos.disco.Disco;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.EstadoProceso;
import edu.unimet.avilaos.modelo.enums.OperacionES;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import edu.unimet.avilaos.modelo.enums.TipoProceso;
import java.io.File;

/**
 * Prueba de integración entre procesos de E/S del núcleo, OperacionesArchivo,
 * SistemaArchivos real y Disco, cargando ejemplos/sistema_archivos_demo.json.
 * En NetBeans: clic derecho sobre este archivo -> Run File.
 */
public class PruebaOperacionesArchivo {

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        ResultadoCarga carga = GestorJsonSistemaArchivos.cargar(
                new File("ejemplos/sistema_archivos_demo.json"), 50, TipoPoliticaDisco.FIFO);
        Disco disco = carga.getDisco();
        SistemaArchivos fs = carga.getSistemaArchivos();

        Cluster cluster = new Cluster(new int[]{1024}, TipoPolitica.FCFS, 3, 50, null);
        cluster.instalarDisco(1, disco, new OperacionesArchivo(fs, disco));
        Computador pc = cluster.getComputador(1);

        long ciclo = 1;

        // 1. user1 lee /usuarios/notas.txt (ok, es suyo)
        System.out.println("=== 1. user1 lee /usuarios/notas.txt ===");
        int atendidasAntes = disco.getPlanificador().getAtendidas();
        PCB p1 = crearProceso("LeerU1", "user1", false, OperacionES.LEER, "/usuarios/notas.txt", 0, null);
        pc.crearProceso(p1, ciclo);
        ciclo = avanzarDesde(pc, disco, new PCB[]{p1}, ciclo, 500);
        verificar("user1 termina lectura de /usuarios/notas.txt", p1.getEstado() == EstadoProceso.TERMINADO);
        verificar("disco atendió la lectura de user1", disco.getPlanificador().getAtendidas() == atendidasAntes + 1);

        // 2. user2 lee /usuarios/notas.txt (rechazado, es privado de user1)
        System.out.println("=== 2. user2 lee /usuarios/notas.txt (rechazado) ===");
        atendidasAntes = disco.getPlanificador().getAtendidas();
        PCB p2 = crearProceso("LeerU2", "user2", false, OperacionES.LEER, "/usuarios/notas.txt", 0, null);
        pc.crearProceso(p2, ciclo);
        ciclo = avanzarDesde(pc, disco, new PCB[]{p2}, ciclo, 200);
        verificar("proceso de user2 termina sin bloquearse", p2.getEstado() == EstadoProceso.TERMINADO);
        verificar("lectura privada rechazada no llegó al disco", disco.getPlanificador().getAtendidas() == atendidasAntes);

        // 3. admin elimina /usuarios/img1.png y se liberan sus bloques
        System.out.println("=== 3. admin elimina /usuarios/img1.png ===");
        int libresAntes = disco.getBloquesLibres();
        Archivo img = (Archivo) fs.buscar("/usuarios/img1.png");
        int bloquesImg = img.getTamanoBloques();
        PCB p3 = crearProceso("DelAdmin", "admin", true, OperacionES.ELIMINAR, "/usuarios/img1.png", 0, null);
        pc.crearProceso(p3, ciclo);
        ciclo = avanzarDesde(pc, disco, new PCB[]{p3}, ciclo, 500);
        verificar("/usuarios/img1.png eliminado", fs.buscar("/usuarios/img1.png") == null);
        verificar("se liberaron los " + bloquesImg + " bloques de img1.png",
                disco.getBloquesLibres() == libresAntes + bloquesImg);

        // 4. user1 crea archivo de 3 bloques en /usuarios/user1 (ok) y en /sistema (rechazado)
        System.out.println("=== 4. user1 crea en /usuarios/user1 (ok) y en /sistema (rechazado) ===");
        libresAntes = disco.getBloquesLibres();
        PCB p4Ok = crearProceso("CrearU1", "user1", false, OperacionES.CREAR, "/usuarios/user1/nuevo.txt", 3, null);
        pc.crearProceso(p4Ok, ciclo);
        ciclo = avanzarDesde(pc, disco, new PCB[]{p4Ok}, ciclo, 500);
        verificar("archivo de 3 bloques creado en /usuarios/user1",
                fs.buscar("/usuarios/user1/nuevo.txt") instanceof Archivo);
        verificar("disco descontó 3 bloques", disco.getBloquesLibres() == libresAntes - 3);

        atendidasAntes = disco.getPlanificador().getAtendidas();
        PCB p4Fail = crearProceso("CrearSist", "user1", false, OperacionES.CREAR, "/sistema/intruso.bin", 3, null);
        pc.crearProceso(p4Fail, ciclo);
        ciclo = avanzarDesde(pc, disco, new PCB[]{p4Fail}, ciclo, 200);
        verificar("no se creó archivo en /sistema", fs.buscar("/sistema/intruso.bin") == null);
        verificar("creación en /sistema no llegó al disco", disco.getPlanificador().getAtendidas() == atendidasAntes);

        // 5. admin renombra un archivo y crea un directorio (bloques = 0)
        System.out.println("=== 5. admin renombra archivo y crea directorio ===");
        PCB p5Ren = crearProceso("RenAdmin", "admin", true, OperacionES.ACTUALIZAR, "/usuarios/user1/nuevo.txt", 0, "renombrado.txt");
        pc.crearProceso(p5Ren, ciclo);
        
        ciclo = avanzarDesde(pc, disco, new PCB[]{p5Ren}, ciclo, 500);
        verificar("nombre anterior ya no existe", fs.buscar("/usuarios/user1/nuevo.txt") == null);
        verificar("archivo renombrado existe", fs.buscar("/usuarios/user1/renombrado.txt") instanceof Archivo);

       PCB p5Dir = crearProceso("DirAdmin", "admin", true, OperacionES.CREAR, "/usuarios/user1/carpeta", 0, null);
        pc.crearProceso(p5Dir, ciclo);
        ciclo = avanzarDesde(pc, disco, new PCB[]{p5Dir}, ciclo, 500);
        verificar("directorio creado con bloques = 0", fs.buscar("/usuarios/user1/carpeta") instanceof Directorio);

        // 6. Persistencia en JSON con bloques no consecutivos (fragmentados)
        System.out.println("=== 6. Persistencia JSON con bloques no consecutivos ===");
        PCB p6A = crearProceso("FragA", "user1", false, OperacionES.CREAR, "/usuarios/user1/fragA.txt", 1, null);
        PCB p6B = crearProceso("FragB", "user1", false, OperacionES.CREAR, "/usuarios/user1/fragB.txt", 1, null);
        PCB p6C = crearProceso("FragC", "user1", false, OperacionES.CREAR, "/usuarios/user1/fragC.txt", 1, null);
        pc.crearProceso(p6A, ciclo);
        pc.crearProceso(p6B, ciclo);
        pc.crearProceso(p6C, ciclo);
        ciclo = avanzarDesde(pc, disco, new PCB[]{p6A, p6B, p6C}, ciclo, 800);

        // Eliminar fragB deja un hueco de 1 bloque en medio; fragD (3 bloques) toma ese hueco y salta fragC
        PCB p6DelB = crearProceso("DelFragB", "user1", false, OperacionES.ELIMINAR, "/usuarios/user1/fragB.txt", 0, null);
        pc.crearProceso(p6DelB, ciclo);
        ciclo = avanzarDesde(pc, disco, new PCB[]{p6DelB}, ciclo, 500);

        PCB p6D = crearProceso("FragD", "user1", false, OperacionES.CREAR, "/usuarios/user1/fragD.txt", 3, null);
        pc.crearProceso(p6D, ciclo);
        ciclo = avanzarDesde(pc, disco, new PCB[]{p6D}, ciclo, 800);

        Archivo fragDOriginal = (Archivo) fs.buscar("/usuarios/user1/fragD.txt");
        int primerBloqueOriginal = fragDOriginal.getPrimerBloque();
        int libresAntesGuardar = disco.getBloquesLibres();

        File tempJson = File.createTempFile("avilaos_test_", ".json");
        tempJson.deleteOnExit();
        GestorJsonSistemaArchivos.guardar(tempJson, fs, disco);

        ResultadoCarga recarga = GestorJsonSistemaArchivos.cargar(tempJson, 50, TipoPoliticaDisco.FIFO);
        Archivo fragDRecargado = (Archivo) recarga.getSistemaArchivos().buscar("/usuarios/user1/fragD.txt");
        verificar("fragD fragmentado recargado desde JSON conserva primer bloque (" + primerBloqueOriginal + ") y 3 bloques",
                fragDRecargado != null
                        && fragDRecargado.getPrimerBloque() == primerBloqueOriginal
                        && fragDRecargado.getTamanoBloques() == 3);
        verificar("bloques libres del disco coinciden tras guardar y recargar JSON",
                recarga.getDisco().getBloquesLibres() == libresAntesGuardar);

        // 7. Preservación de dueño / creador en JSON
        System.out.println("=== 7. Preservación de dueño/creador tras persistencia JSON ===");
        Archivo renRecargado = (Archivo) recarga.getSistemaArchivos().buscar("/usuarios/user1/renombrado.txt");
        verificar("dueño 'user1' se preserva en JSON",
                renRecargado != null && "user1".equals(renRecargado.getDueno()));

        // 8. Eliminación recursiva de directorio con archivos internos libera todos sus bloques
        // 8. Eliminación de directorio con archivos internos libera todos sus bloques
        System.out.println("=== 8. Eliminación de directorio libera bloques internos ===");
        libresAntes = disco.getBloquesLibres();
        PCB p8Sub1 = crearProceso("Sub1", "admin", true, OperacionES.CREAR, "/usuarios/user1/carpeta/sub1.txt", 2, null);
        PCB p8Sub2 = crearProceso("Sub2", "admin", true, OperacionES.CREAR, "/usuarios/user1/carpeta/sub2.txt", 2, null);
        pc.crearProceso(p8Sub1, ciclo);
        pc.crearProceso(p8Sub2, ciclo);
        ciclo = avanzarDesde(pc, disco, new PCB[]{p8Sub1, p8Sub2}, ciclo, 800);
        verificar("se ocuparon 4 bloques dentro de /usuarios/user1/carpeta",
                disco.getBloquesLibres() == libresAntes - 4);

        PCB p8Del1 = crearProceso("DelSub1", "admin", true, OperacionES.ELIMINAR, "/usuarios/user1/carpeta/sub1.txt", 0, null);
        PCB p8Del2 = crearProceso("DelSub2", "admin", true, OperacionES.ELIMINAR, "/usuarios/user1/carpeta/sub2.txt", 0, null);
        pc.crearProceso(p8Del1, ciclo);
        pc.crearProceso(p8Del2, ciclo);
        ciclo = avanzarDesde(pc, disco, new PCB[]{p8Del1, p8Del2}, ciclo, 800);

        PCB p8DelDir = crearProceso("DelCarpeta", "admin", true, OperacionES.ELIMINAR, "/usuarios/user1/carpeta", 0, null);
        pc.crearProceso(p8DelDir, ciclo);
        avanzarDesde(pc, disco, new PCB[]{p8DelDir}, ciclo, 800);
        verificar("directorio y archivos internos eliminados",
                fs.buscar("/usuarios/user1/carpeta") == null
                        && fs.buscar("/usuarios/user1/carpeta/sub1.txt") == null
                        && fs.buscar("/usuarios/user1/carpeta/sub2.txt") == null);
        verificar("eliminación devolvió los 4 bloques al disco",
                disco.getBloquesLibres() == libresAntes);
    }

    private static PCB crearProceso(String nombre, String usuario, boolean admin,
                                    OperacionES op, String ruta, int bloques, String nuevoNombre) {
        PCB p = new PCB(nombre, TipoProceso.ES_ARCHIVO, 1, 16, 2, 500, 1, 0);
        p.setUsuario(usuario, admin);
        p.configurarArchivo(op, ruta, bloques);
        if (nuevoNombre != null) {
            p.setNuevoNombre(nuevoNombre);
        }
        return p;
    }

    private static long avanzarDesde(Computador pc, Disco disco, PCB[] procesos, long cicloInicio, long maxPasos) {
        long ciclo = cicloInicio;
        for (long pasos = 0; pasos < maxPasos && terminados(procesos) < procesos.length; pasos++, ciclo++) {
            pc.tick(ciclo);
            disco.tick(ciclo);
        }
        return ciclo;
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
}