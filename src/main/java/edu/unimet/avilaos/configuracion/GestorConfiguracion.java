package edu.unimet.avilaos.configuracion;

import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.gui.SolicitudProceso;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import edu.unimet.avilaos.modelo.enums.TipoProceso;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Lee y escribe la configuracion en un archivo CSV. Cada linea empieza con su tipo:
 *
 * parametro,duracion_ms,500
 * parametro,quantum,3
 * parametro,latencia,3
 * computador,1,256,FCFS
 * buffer,B1,5,1
 * proceso,nombre,tipo,prioridad,memoria,instrucciones,deadline,computador,esCada,esDura,buffer,ciclosPorElemento,elementos
 *
 * En "proceso", computador -1 significa asignacion automatica. Las lineas que empiezan con # se ignoran.
 */
public final class GestorConfiguracion {

    /** Archivo que se carga solo al abrir el programa, si existe. */
    public static final File ARCHIVO_POR_DEFECTO = new File("avilaos_config.csv");

    private GestorConfiguracion() {
    }

    public static void guardar(ConfiguracionSimulacion c, File archivo) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(archivo.toPath(), StandardCharsets.UTF_8)) {
            w.write("# Configuracion de AvilaOS");
            w.newLine();
            escribir(w, "parametro,duracion_ms," + c.getDuracionCicloMs());
            escribir(w, "parametro,quantum," + c.getQuantum());
            escribir(w, "parametro,latencia," + c.getLatencia());
            for (int i = 0; i < c.getCantidadComputadores(); i++) {
                escribir(w, "computador," + (i + 1) + "," + c.getRamPorComputador()[i] + "," + c.getPoliticas()[i].name());
            }
            for (int i = 0; i < c.getBuffers().tamano(); i++) {
                BufferInicial b = c.getBuffers().obtener(i);
                escribir(w, "buffer," + limpiar(b.id()) + "," + b.capacidad() + "," + b.idAnfitrion());
            }
            for (int i = 0; i < c.getProcesos().tamano(); i++) {
                SolicitudProceso p = c.getProcesos().obtener(i);
                escribir(w, "proceso," + limpiar(p.nombre()) + "," + p.tipo().name() + "," + p.prioridad() + ","
                        + p.memoria() + "," + p.instrucciones() + "," + p.deadline() + "," + p.idComputador() + ","
                        + p.ciclosEntreES() + "," + p.ciclosParaSatisfacerES() + ","
                        + (p.idBuffer() == null ? "" : limpiar(p.idBuffer())) + ","
                        + p.ciclosPorElemento() + "," + p.elementosRequeridos());
            }
        }
    }

    /** Lee el archivo. Si una linea esta mal, lanza IllegalArgumentException diciendo cual. */
    public static ConfiguracionSimulacion cargar(File archivo) throws IOException {
        ConfiguracionSimulacion c = ConfiguracionSimulacion.porDefecto();
        ListaEnlazada<Integer> rams = new ListaEnlazada<>();
        ListaEnlazada<TipoPolitica> politicas = new ListaEnlazada<>();
        ListaEnlazada<SolicitudProceso> procesos = new ListaEnlazada<>();
        ListaEnlazada<BufferInicial> buffers = new ListaEnlazada<>();

        try (BufferedReader r = Files.newBufferedReader(archivo.toPath(), StandardCharsets.UTF_8)) {
            String linea;
            int numero = 0;
            while ((linea = r.readLine()) != null) {
                numero++;
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }
                String[] p = linea.split(",", -1);
                try {
                    switch (p[0]) {
                        case "parametro":
                            exigir(p, 3);
                            int valor = entero(p[2]);
                            switch (p[1]) {
                                case "duracion_ms": c.setDuracionCicloMs(valor); break;
                                case "quantum": c.setQuantum(valor); break;
                                case "latencia": c.setLatencia(valor); break;
                                default: throw new IllegalArgumentException("parámetro desconocido \"" + p[1] + "\"");
                            }
                            break;
                        case "computador":
                            exigir(p, 4);
                            rams.agregar(entero(p[2]));
                            politicas.agregar(TipoPolitica.valueOf(p[3]));
                            break;
                        case "buffer":
                            exigir(p, 4);
                            buffers.agregar(new BufferInicial(p[1], entero(p[2]), entero(p[3])));
                            break;
                        case "proceso":
                            exigir(p, 13);
                            procesos.agregar(new SolicitudProceso(p[1], TipoProceso.valueOf(p[2]), entero(p[3]),
                                    entero(p[4]), entero(p[5]), entero(p[6]), entero(p[7]), entero(p[8]), entero(p[9]),
                                    p[10].isEmpty() ? null : p[10], entero(p[11]), entero(p[12])));
                            break;
                        default:
                            throw new IllegalArgumentException("tipo de línea desconocido \"" + p[0] + "\"");
                    }
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Error en la línea " + numero + " del archivo: " + ex.getMessage());
                }
            }
        }

        if (rams.tamano() > 0) {
            int[] ram = new int[rams.tamano()];
            TipoPolitica[] pol = new TipoPolitica[rams.tamano()];
            for (int i = 0; i < ram.length; i++) {
                ram[i] = rams.obtener(i);
                pol[i] = politicas.obtener(i);
            }
            c.setComputadores(ram, pol);
        }
        c.setProcesos(procesos);
        c.setBuffers(buffers);
        c.validar();
        return c;
    }

    private static void escribir(BufferedWriter w, String linea) throws IOException {
        w.write(linea);
        w.newLine();
    }

    /** Las comas romperian el CSV, asi que se cambian por espacios. */
    private static String limpiar(String texto) {
        return texto.replace(',', ' ');
    }

    private static void exigir(String[] partes, int cantidad) {
        if (partes.length != cantidad) {
            throw new IllegalArgumentException("se esperaban " + cantidad + " valores y hay " + partes.length);
        }
    }

    private static int entero(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("\"" + texto + "\" no es un número entero");
        }
    }
}
