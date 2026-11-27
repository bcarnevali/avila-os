package edu.unimet.avilaos.archivos;

import edu.unimet.avilaos.disco.Disco;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.ModoUsuario;
import edu.unimet.avilaos.modelo.enums.TipoPermiso;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import java.io.File;
import java.nio.file.Files;

public class PruebaSistemaArchivos {

    public static void main(String[] args) throws Exception {
        Disco disco = new Disco(100, 0, 0, DireccionCabezal.ARRIBA, 1, TipoPoliticaDisco.FIFO, null);
        SistemaArchivos sa = new SistemaArchivos(disco);

        // 1. Crear directorios y archivos
        Directorio dirUsuarios = sa.crearDirectorio("/usuarios", "admin", TipoPermiso.PUBLICO);
        Directorio dirPrivado = sa.crearDirectorio("/usuarios/user1", "user1", TipoPermiso.PRIVADO);
        Archivo notas = sa.crearArchivo("/usuarios/user1/notas.txt", 10, "user1", TipoPermiso.PRIVADO, "Proc_1");
        Archivo readme = sa.crearArchivo("/usuarios/readme.md", 5, "admin", TipoPermiso.PUBLICO, "Proc_2");

        verificar(sa.buscar("/usuarios/user1/notas.txt") == notas, "Debe encontrar notas.txt");
        verificar(notas.getRuta().equals("/usuarios/user1/notas.txt"), "Ruta debe construirse desde padres");
        verificar(disco.getBloquesLibres() == 85, "Deben quedar 85 bloques libres tras asignar 15");

        // 2. Renombrar archivo y directorio (actualizando rutas en disco)
        sa.renombrar("/usuarios/user1/notas.txt", "apuntes.txt");
        verificar(sa.buscar("/usuarios/user1/apuntes.txt") == notas, "Debe encontrar apuntes.txt tras renombrar");
        verificar("/usuarios/user1/apuntes.txt".equals(disco.getOcupante(notas.getPrimerBloque())),
                "El disco debe reflejar la nueva ruta");

        sa.renombrar("/usuarios/user1", "u1");
        verificar("/usuarios/u1/apuntes.txt".equals(notas.getRuta()), "Ruta hija debe actualizarse al renombrar padre");

        // 3. Permisos
        verificar(Permisos.motivoParaLeer(ModoUsuario.USUARIO, "user2", notas) != null,
                "user2 no debe leer archivo privado de user1");
        verificar(Permisos.motivoParaLeer(ModoUsuario.USUARIO, "user2", readme) == null,
                "user2 sí debe leer archivo público");
        verificar(Permisos.motivoParaLeer(ModoUsuario.ADMINISTRADOR, "admin", notas) == null,
                "admin debe leer todo");
        verificar(Permisos.motivoParaCrear(ModoUsuario.USUARIO, "user2", dirPrivado) != null,
                "user2 no puede crear en directorio de user1");
        verificar(Permisos.motivoParaCrear(ModoUsuario.USUARIO, "user1", dirPrivado) == null,
                "user1 sí puede crear en su directorio");
        verificar(Permisos.motivoParaRenombrar(ModoUsuario.USUARIO, "user1", notas) != null,
                "Solo admin puede renombrar");
        verificar(Permisos.motivoParaEliminar(ModoUsuario.USUARIO, "user1", dirUsuarios) != null,
                "Usuario estándar no puede eliminar directorios");

        // 4. Rechazar archivo sin espacio
        boolean falloSinEspacio = false;
        try {
            sa.crearArchivo("/usuarios/gigante.bin", 95, "admin", TipoPermiso.PUBLICO, "Proc_3");
        } catch (IllegalStateException e) {
            falloSinEspacio = true;
        }
        verificar(falloSinEspacio, "Debe lanzar IllegalStateException si no hay espacio");

        // 5. Eliminar directorio recursivamente y liberar bloques
        sa.eliminar("/usuarios");
        verificar(sa.buscar("/usuarios") == null, "Directorio /usuarios debe desaparecer");
        verificar(sa.buscar("/usuarios/u1/apuntes.txt") == null, "Hijos deben desaparecer");
        verificar(disco.getBloquesLibres() == 100, "Todos los bloques deben quedar libres tras eliminar");

        // 6. Probar carga y guardado JSON
        String jsonEjemplo = "{\n"
                + "  \"diskConfig\": { \"totalBlocks\": 200, \"initialHead\": 50 },\n"
                + "  \"fileSystem\": [\n"
                + "    { \"path\": \"/sistema\", \"type\": \"DIRECTORY\", \"owner\": \"admin\", \"permissions\": \"public\" },\n"
                + "    { \"path\": \"/sistema/boot.bin\", \"type\": \"FILE\", \"startBlock\": 11, \"blocks\": 23, \"owner\": \"admin\", \"permissions\": \"public\" }\n"
                + "  ]\n"
                + "}";
        File tempIn = Files.createTempFile("fs_in", ".json").toFile();
        File tempOut = Files.createTempFile("fs_out", ".json").toFile();
        Files.write(tempIn.toPath(), jsonEjemplo.getBytes());

        GestorJsonSistemaArchivos.ResultadoCarga carga = GestorJsonSistemaArchivos.cargar(tempIn);
        verificar(carga.getSistemaArchivos().buscar("/sistema/boot.bin") != null, "JSON debe cargar boot.bin");
        GestorJsonSistemaArchivos.guardar(tempOut, carga.getSistemaArchivos(), carga.getDisco());
        verificar(tempOut.length() > 0, "JSON guardado no debe estar vacío");

        System.out.println("TODAS LAS PRUEBAS PASARON");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError("Fallo: " + mensaje);
        }
    }
}