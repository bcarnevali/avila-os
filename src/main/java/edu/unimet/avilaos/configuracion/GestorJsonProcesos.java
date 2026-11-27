package edu.unimet.avilaos.configuracion;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.OperacionES;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import edu.unimet.avilaos.modelo.enums.TipoPoliticaDisco;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Lee el JSON de procesos y configuración con el formato del enunciado:
 * globalConfig (cycleDurationMs, swapSizeBlocks, initialDiskPolicy),
 * computer (ramCapacity, policy, quantum) y processes (name, memory, deadline,
 * instructions, targetPath, operation, fileSizeBlocks).
 *
 * Todo dato inválido se rechaza con IllegalArgumentException y un mensaje que
 * dice qué campo está mal, para mostrarlo en la interfaz.
 */
public final class GestorJsonProcesos {

    public static final int DURACION_MIN = 50;
    public static final int DURACION_MAX = 5000;
    public static final int RAM_MIN = 16;
    public static final int RAM_MAX = 100000;
    public static final int QUANTUM_POR_DEFECTO = 3;
    public static final int PRIORIDAD_POR_DEFECTO = 5;

    private GestorJsonProcesos() {
    }

    // ===================== Formato del archivo (lo llena Gson) =====================

    private static class ArchivoDto {
        GlobalDto globalConfig;
        ComputadorDto computer;
        ProcesoDto[] processes;
    }

    private static class GlobalDto {
        Integer cycleDurationMs;
        Integer swapSizeBlocks;
        String initialDiskPolicy;
    }

    private static class ComputadorDto {
        Integer ramCapacity;
        String policy;
        Integer quantum;
    }

    private static class ProcesoDto {
        String name;
        Integer memory;
        Integer deadline;
        Integer instructions;
        Integer priority; // opcional: el formato del enunciado no lo trae
        String targetPath;
        String operation;
        Integer fileSizeBlocks;
    }

    // ===================== Lectura =====================

    public static ConfiguracionProcesos cargar(File archivo) throws IOException {
        return leer(new String(Files.readAllBytes(archivo.toPath()), StandardCharsets.UTF_8));
    }

    public static ConfiguracionProcesos leer(String json) {
        ArchivoDto dto;
        try {
            dto = new Gson().fromJson(json, ArchivoDto.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("El archivo no es un JSON válido o tiene un dato con el tipo"
                    + " equivocado (por ejemplo, letras donde va un número).");
        }
        if (dto == null) {
            throw new IllegalArgumentException("El archivo JSON está vacío.");
        }
        if (dto.globalConfig == null) {
            throw new IllegalArgumentException("Falta la sección \"globalConfig\".");
        }
        if (dto.computer == null) {
            throw new IllegalArgumentException("Falta la sección \"computer\".");
        }
        if (dto.processes == null) {
            throw new IllegalArgumentException("Falta la lista \"processes\".");
        }

        GlobalDto g = dto.globalConfig;
        int duracion = entero(g.cycleDurationMs, "", "globalConfig.cycleDurationMs", DURACION_MIN, DURACION_MAX);
        int swap = entero(g.swapSizeBlocks, "", "globalConfig.swapSizeBlocks", 0, 499);
        TipoPoliticaDisco politicaDisco = politicaDisco(g.initialDiskPolicy);

        ComputadorDto c = dto.computer;
        int ram = entero(c.ramCapacity, "", "computer.ramCapacity", RAM_MIN, RAM_MAX);
        TipoPolitica politica = politicaCpu(c.policy);
        int quantum;
        if (politica == TipoPolitica.ROUND_ROBIN) {
            quantum = entero(c.quantum, "Round Robin necesita quantum: ", "computer.quantum", 1, 100);
        } else {
            // las demás políticas no usan quantum: el formato trae 0 y se deja uno por defecto
            quantum = c.quantum != null && c.quantum > 0 ? c.quantum : QUANTUM_POR_DEFECTO;
        }

        ListaEnlazada<ProcesoJson> procesos = new ListaEnlazada<>();
        for (int i = 0; i < dto.processes.length; i++) {
            procesos.agregar(proceso(dto.processes[i], i + 1, ram));
        }
        return new ConfiguracionProcesos(duracion, swap, politicaDisco, ram, politica, quantum, procesos);
    }

    private static ProcesoJson proceso(ProcesoDto p, int numero, int ram) {
        if (p == null) {
            throw new IllegalArgumentException("El proceso #" + numero + " está vacío.");
        }
        String nombre = p.name == null || p.name.isBlank() ? null : p.name.trim();
        if (nombre == null) {
            throw new IllegalArgumentException("El proceso #" + numero + " no tiene \"name\".");
        }
        String quien = "Proceso #" + numero + " (" + nombre + "): ";
        int memoria = entero(p.memory, quien, "memory", 1, ram);
        int deadline = entero(p.deadline, quien, "deadline", 1, 1_000_000);
        int instrucciones = entero(p.instructions, quien, "instructions", 1, 1_000_000);
        int prioridad = p.priority == null ? PRIORIDAD_POR_DEFECTO : entero(p.priority, quien, "priority", 1, 10);
        if (p.targetPath == null || !p.targetPath.trim().startsWith("/")) {
            throw new IllegalArgumentException(quien + "\"targetPath\" debe ser una ruta que empiece con /.");
        }
        OperacionES operacion = operacion(p.operation, quien);
        int bloques = p.fileSizeBlocks == null ? 0 : entero(p.fileSizeBlocks, quien, "fileSizeBlocks", 0, 500);
        if (operacion == OperacionES.CREAR && bloques < 1) {
            throw new IllegalArgumentException(quien + "para CREATE \"fileSizeBlocks\" debe ser al menos 1.");
        }
        return new ProcesoJson(nombre, memoria, deadline, instrucciones, prioridad,
                p.targetPath.trim(), operacion, bloques);
    }

    // ===================== Conversión de textos a enums =====================

    private static TipoPoliticaDisco politicaDisco(String texto) {
        String t = normalizar(texto, "", "globalConfig.initialDiskPolicy");
        switch (t) {
            case "FIFO": case "FCFS": return TipoPoliticaDisco.FIFO;
            case "SSTF": return TipoPoliticaDisco.SSTF;
            case "SCAN": return TipoPoliticaDisco.SCAN;
            case "CSCAN": return TipoPoliticaDisco.C_SCAN;
            default: throw new IllegalArgumentException("Política de disco desconocida: \"" + texto
                    + "\". Use FIFO, SSTF, SCAN o C-SCAN.");
        }
    }

    private static TipoPolitica politicaCpu(String texto) {
        String t = normalizar(texto, "", "computer.policy");
        switch (t) {
            case "FCFS": case "FIFO": return TipoPolitica.FCFS;
            case "RR": case "ROUNDROBIN": return TipoPolitica.ROUND_ROBIN;
            case "EDF": return TipoPolitica.EDF;
            case "PRIORIDAD": case "PRIORITY": case "PRIORIDADAPROPIATIVA": return TipoPolitica.PRIORIDAD_APROPIATIVA;
            default: throw new IllegalArgumentException("Política de CPU desconocida: \"" + texto
                    + "\". Use FCFS, ROUND_ROBIN, EDF o PRIORIDAD.");
        }
    }

    private static OperacionES operacion(String texto, String quien) {
        String t = normalizar(texto, quien, "operation");
        switch (t) {
            case "CREATE": case "CREAR": return OperacionES.CREAR;
            case "READ": case "LEER": return OperacionES.LEER;
            case "UPDATE": case "ACTUALIZAR": return OperacionES.ACTUALIZAR;
            case "DELETE": case "ELIMINAR": return OperacionES.ELIMINAR;
            default: throw new IllegalArgumentException(quien + "operación desconocida \"" + texto
                    + "\". Use CREATE, READ, UPDATE o DELETE.");
        }
    }

    /** Mayúsculas y sin espacios, guiones ni guiones bajos ("c-scan" → "CSCAN"). */
    private static String normalizar(String texto, String prefijo, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(mensaje(prefijo, "falta el campo \"" + campo + "\"."));
        }
        StringBuilder sb = new StringBuilder();
        for (char ch : texto.trim().toUpperCase().toCharArray()) {
            if (ch != '-' && ch != '_' && ch != ' ') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static int entero(Integer valor, String prefijo, String campo, int minimo, int maximo) {
        if (valor == null) {
            throw new IllegalArgumentException(mensaje(prefijo, "falta el campo \"" + campo + "\"."));
        }
        if (valor < minimo || valor > maximo) {
            throw new IllegalArgumentException(mensaje(prefijo, "el campo \"" + campo + "\" debe estar entre "
                    + minimo + " y " + maximo + " (vale " + valor + ")."));
        }
        return valor;
    }

    /** Sin prefijo, el mensaje empieza con mayúscula. */
    private static String mensaje(String prefijo, String texto) {
        if (prefijo.isEmpty()) {
            return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
        }
        return prefijo + texto;
    }
}
