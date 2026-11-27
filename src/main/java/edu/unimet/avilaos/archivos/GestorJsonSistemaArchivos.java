package edu.unimet.avilaos.archivos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import edu.unimet.avilaos.disco.Disco;
import edu.unimet.avilaos.kernel.RegistroEventos;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.DireccionCabezal;
import edu.unimet.avilaos.modelo.enums.TipoPermiso;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class GestorJsonSistemaArchivos {

    public static class ResultadoCarga {
        private final int totalBloques;
        private final int cabezalInicial;
        private final Disco disco;
        private final SistemaArchivos sistemaArchivos;

        public ResultadoCarga(int totalBloques, int cabezalInicial, Disco disco, SistemaArchivos sistemaArchivos) {
            this.totalBloques = totalBloques;
            this.cabezalInicial = cabezalInicial;
            this.disco = disco;
            this.sistemaArchivos = sistemaArchivos;
        }

        public int getTotalBloques() { return totalBloques; }
        public int getCabezalInicial() { return cabezalInicial; }
        public Disco getDisco() { return disco; }
        public SistemaArchivos getSistemaArchivos() { return sistemaArchivos; }
    }

    private GestorJsonSistemaArchivos() {
    }

    private static class ArchivoSistemaDto {
        DiskConfigDto diskConfig;
        EntradaDto[] fileSystem;
    }

    private static class DiskConfigDto {
        Integer totalBlocks;
        Integer initialHead;
    }

    private static class EntradaDto {
        String path;
        String type;
        Integer startBlock;
        Integer blocks;
        int[] allocatedBlocks;
        String creator;
        String owner;
        String permissions;
    }

    public static ResultadoCarga cargar(File archivo) throws IOException {
        return cargar(archivo, 0, TipoPoliticaDisco.FIFO);
    }

    public static ResultadoCarga cargar(File archivo, int tamanoSwap, TipoPoliticaDisco politica) throws IOException {
        return cargar(archivo, tamanoSwap, politica, null);
    }

    public static ResultadoCarga cargar(File archivo, int tamanoSwap, TipoPoliticaDisco politica, RegistroEventos log) throws IOException {
        String json = new String(Files.readAllBytes(archivo.toPath()), StandardCharsets.UTF_8);
        return leer(json, tamanoSwap, politica, log);
    }

    public static ResultadoCarga leer(String json, int tamanoSwap, TipoPoliticaDisco politica) {
        return leer(json, tamanoSwap, politica, null);
    }

    public static ResultadoCarga leer(String json, int tamanoSwap, TipoPoliticaDisco politica, RegistroEventos log) {
        ArchivoSistemaDto dto;
        try {
            dto = new Gson().fromJson(json, ArchivoSistemaDto.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("El archivo no es un JSON válido.");
        }
        if (dto == null || dto.diskConfig == null) {
            throw new IllegalArgumentException("Falta la sección \"diskConfig\" en el JSON.");
        }
        if (dto.fileSystem == null) {
            throw new IllegalArgumentException("Falta la lista \"fileSystem\" en el JSON.");
        }
        if (dto.diskConfig.totalBlocks == null || dto.diskConfig.initialHead == null) {
            throw new IllegalArgumentException("Faltan campos en \"diskConfig\".");
        }

        int totalBloques = dto.diskConfig.totalBlocks;
        int cabezalInicial = dto.diskConfig.initialHead;
        if (cabezalInicial < 0 || cabezalInicial >= totalBloques) {
            throw new IllegalArgumentException("initialHead fuera de rango.");
        }

        Disco disco = new Disco(totalBloques, tamanoSwap, cabezalInicial,
                DireccionCabezal.ARRIBA, 1, politica, log);
        SistemaArchivos sistema = new SistemaArchivos(disco);

        // 1. Primero directorios
        for (int i = 0; i < dto.fileSystem.length; i++) {
            EntradaDto e = dto.fileSystem[i];
            validarBasico(e, i + 1);
            if ("DIRECTORY".equalsIgnoreCase(e.type.trim())) {
                sistema.crearDirectorio(e.path.trim(), e.owner.trim(), parsePermiso(e.permissions, i + 1));
            }
        }

        // 2. Después archivos
        for (int i = 0; i < dto.fileSystem.length; i++) {
            EntradaDto e = dto.fileSystem[i];
            String tipo = e.type.trim().toUpperCase();
            if ("FILE".equals(tipo)) {
                if (e.startBlock == null || e.startBlock < 0 || e.startBlock >= totalBloques) {
                    throw new IllegalArgumentException("Entrada #" + (i + 1) + ": startBlock inválido.");
                }
                if (e.blocks == null || e.blocks < 1) {
                    throw new IllegalArgumentException("Entrada #" + (i + 1) + ": blocks inválido.");
                }
                sistema.cargarArchivo(e.path.trim(), e.startBlock, e.blocks,
                        e.owner.trim(), parsePermiso(e.permissions, i + 1), e.allocatedBlocks, e.creator);
            } else if (!"DIRECTORY".equals(tipo)) {
                throw new IllegalArgumentException("Entrada #" + (i + 1) + ": tipo desconocido \"" + e.type + "\".");
            }
        }

        return new ResultadoCarga(totalBloques, cabezalInicial, disco, sistema);
    }

    public static void guardar(File destino, SistemaArchivos sistema, Disco disco) throws IOException {
        ArchivoSistemaDto dto = new ArchivoSistemaDto();
        dto.diskConfig = new DiskConfigDto();
        dto.diskConfig.totalBlocks = disco.getTotalBloques();
        dto.diskConfig.initialHead = disco.getPlanificador().getPosicion();

        ListaEnlazada<EntradaDto> entradas = new ListaEnlazada<>();
        recolectarPreorden(sistema.getRaiz(), entradas, disco);

        dto.fileSystem = new EntradaDto[entradas.tamano()];
        for (int i = 0; i < entradas.tamano(); i++) {
            dto.fileSystem[i] = entradas.obtener(i);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.write(destino.toPath(), gson.toJson(dto).getBytes(StandardCharsets.UTF_8));
    }

    private static void recolectarPreorden(Directorio dir, ListaEnlazada<EntradaDto> salida, Disco disco) {
        ListaEnlazada<EntradaSistemaArchivos> hijos = dir.getHijos();
        for (int i = 0; i < hijos.tamano(); i++) {
            EntradaSistemaArchivos hijo = hijos.obtener(i);
            EntradaDto item = new EntradaDto();
            item.path = hijo.getRuta();
            item.owner = hijo.getDueno();
            item.permissions = (hijo.getPermiso() == TipoPermiso.PRIVADO) ? "private" : "public";

            if (hijo instanceof Directorio) {
                item.type = "DIRECTORY";
                salida.agregar(item);
                recolectarPreorden((Directorio) hijo, salida, disco);
            } else if (hijo instanceof Archivo) {
                Archivo arch = (Archivo) hijo;
                item.type = "FILE";
                item.startBlock = arch.getPrimerBloque();
                item.blocks = arch.getTamanoBloques();
                item.allocatedBlocks = disco.getCadena(arch.getPrimerBloque());
                item.creator = arch.getProcesoCreador();
                salida.agregar(item);
            }
        }
    }

    private static void validarBasico(EntradaDto e, int num) {
        if (e == null || e.path == null || !e.path.trim().startsWith("/")) {
            throw new IllegalArgumentException("Entrada #" + num + ": ruta inválida.");
        }
        if (e.type == null || e.type.isBlank() || e.owner == null || e.owner.isBlank()) {
            throw new IllegalArgumentException("Entrada #" + num + ": faltan type u owner.");
        }
    }

    private static TipoPermiso parsePermiso(String texto, int num) {
        if (texto == null || texto.isBlank()) {
            return TipoPermiso.PUBLICO;
        }
        String t = texto.trim().toLowerCase();
        if ("private".equals(t) || "privado".equals(t)) {
            return TipoPermiso.PRIVADO;
        }
        if ("public".equals(t) || "publico".equals(t)) {
            return TipoPermiso.PUBLICO;
        }
        throw new IllegalArgumentException("Entrada #" + num + ": permiso inválido \"" + texto + "\".");
    }
}