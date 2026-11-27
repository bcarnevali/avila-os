package edu.unimet.avilaos.archivos;

import edu.unimet.avilaos.disco.Disco;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.modelo.enums.TipoPermiso;
import java.util.concurrent.Semaphore;

public class SistemaArchivos {

    private final Disco disco;
    private final Directorio raiz;
    private final Semaphore mutex;

    public SistemaArchivos(Disco disco) {
        this.disco = disco;
        this.raiz = new Directorio("/", "admin", TipoPermiso.PUBLICO, null);
        this.mutex = new Semaphore(1);
    }

    private void bloquear() {     
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido esperando el sistema de archivos", e);
        }
 
    }

    public Directorio getRaiz() {
        return raiz;
    }

    public EntradaSistemaArchivos buscar(String ruta) {
        bloquear();
        try {
            return buscarInterno(ruta);
        } finally {
            mutex.release();
        }
    }

    private EntradaSistemaArchivos buscarInterno(String ruta) {
        if (ruta == null || ruta.trim().isEmpty()) {
            return null;
        }
        String limpia = normalizarRuta(ruta);
        if ("/".equals(limpia)) {
            return raiz;
        }
        String[] partes = limpia.substring(1).split("/");
        EntradaSistemaArchivos actual = raiz;
        for (String parte : partes) {
            if (!(actual instanceof Directorio)) {
                return null;
            }
            actual = ((Directorio) actual).buscarHijo(parte);
            if (actual == null) {
                return null;
            }
        }
        return actual;
    }

    public Directorio crearDirectorio(String ruta, String dueno, TipoPermiso permiso) {
        bloquear();
        try {
            String limpia = normalizarRuta(ruta);
            if ("/".equals(limpia)) {
                throw new IllegalArgumentException("No se puede crear la raíz nuevamente.");
            }
            if (buscarInterno(limpia) != null) {
                throw new IllegalArgumentException("Ya existe una entrada en la ruta: " + limpia);
            }
            Directorio padre = obtenerDirectorioPadre(limpia);
            String nombre = obtenerNombreFinal(limpia);
            Directorio nuevo = new Directorio(nombre, dueno, permiso, padre);
            padre.agregarHijo(nuevo);
            return nuevo;
        } finally {
            mutex.release();
        }
    }

    public Archivo crearArchivo(String ruta, int bloques, String dueno, TipoPermiso permiso, String procesoCreador) {
        bloquear();
        try {
            String limpia = normalizarRuta(ruta);
            if ("/".equals(limpia)) {
                throw new IllegalArgumentException("Ruta inválida para archivo.");
            }
            if (buscarInterno(limpia) != null) {
                throw new IllegalArgumentException("Ya existe una entrada en la ruta: " + limpia);
            }
            Directorio padre = obtenerDirectorioPadre(limpia);
            String nombre = obtenerNombreFinal(limpia);

            // Si no hay espacio, disco lanza IllegalStateException y la dejamos pasar
            int primerBloque = disco.asignarArchivo(limpia, bloques);

            Archivo nuevo = new Archivo(nombre, dueno, permiso, padre, primerBloque, bloques, procesoCreador);
            padre.agregarHijo(nuevo);
            return nuevo;
        } finally {
            mutex.release();
        }
    }

    public Archivo cargarArchivo(String ruta, int primerBloque, int bloques, String dueno, TipoPermiso permiso) {
        return cargarArchivo(ruta, primerBloque, bloques, dueno, permiso, null, "JSON");
    }

    public Archivo cargarArchivo(String ruta, int primerBloque, int bloques, String dueno,
                                 TipoPermiso permiso, int[] cadena, String creador) {
        bloquear();
        try {
            String limpia = normalizarRuta(ruta);
            if (buscarInterno(limpia) != null) {
                throw new IllegalArgumentException("Ya existe una entrada en la ruta: " + limpia);
            }
            Directorio padre = obtenerDirectorioPadre(limpia);
            String nombre = obtenerNombreFinal(limpia);
            if (cadena == null) disco.asignarArchivoEn(primerBloque, limpia, bloques);
            else {
                if (cadena.length != bloques || bloques < 1 || cadena[0] != primerBloque)
                    throw new IllegalArgumentException("Cadena incompatible con " + ruta);
                disco.restaurarCadena(limpia, cadena);
            }

            Archivo nuevo = new Archivo(nombre, dueno, permiso, padre, primerBloque, bloques, creador == null ? "JSON" : creador);
            padre.agregarHijo(nuevo);
            return nuevo;
        } finally {
            mutex.release();
        }
    }

    public void renombrar(String ruta, String nuevoNombre) {
        bloquear();
        try {
            if (nuevoNombre == null || nuevoNombre.trim().isEmpty() || nuevoNombre.contains("/")) {
                throw new IllegalArgumentException("Nombre inválido: " + nuevoNombre);
            }
            String limpia = normalizarRuta(ruta);
            if ("/".equals(limpia)) {
                throw new IllegalArgumentException("No se puede renombrar la raíz.");
            }
            EntradaSistemaArchivos entrada = buscarInterno(limpia);
            if (entrada == null) {
                throw new IllegalArgumentException("No existe la ruta: " + limpia);
            }
            Directorio padre = entrada.getPadre();
            if (padre.buscarHijo(nuevoNombre) != null) {
                throw new IllegalArgumentException("Ya existe '" + nuevoNombre + "' en " + padre.getRuta());
            }
            entrada.setNombre(nuevoNombre);
            actualizarRutasEnDisco(entrada);
        } finally {
            mutex.release();
        }
    }

    private void actualizarRutasEnDisco(EntradaSistemaArchivos entrada) {
        if (entrada instanceof Archivo) {
            Archivo arch = (Archivo) entrada;
            disco.renombrarOcupante(arch.getPrimerBloque(), arch.getRuta());
        } else if (entrada instanceof Directorio) {
            Directorio dir = (Directorio) entrada;
            ListaEnlazada<EntradaSistemaArchivos> hijos = dir.getHijos();
            for (int i = 0; i < hijos.tamano(); i++) {
                actualizarRutasEnDisco(hijos.obtener(i));
            }
        }
    }

    public void eliminar(String ruta) {
        bloquear();
        try {
            String limpia = normalizarRuta(ruta);
            if ("/".equals(limpia)) {
                throw new IllegalArgumentException("No se puede eliminar la raíz.");
            }
            EntradaSistemaArchivos entrada = buscarInterno(limpia);
            if (entrada == null) {
                throw new IllegalArgumentException("No existe la ruta: " + limpia);
            }
            eliminarRecursivo(entrada);
            entrada.getPadre().eliminarHijo(entrada);
        } finally {
            mutex.release();
        }
    }

    private void eliminarRecursivo(EntradaSistemaArchivos entrada) {
        if (entrada instanceof Archivo) {
            Archivo arch = (Archivo) entrada;
            disco.liberar(arch.getPrimerBloque());
        } else if (entrada instanceof Directorio) {
            Directorio dir = (Directorio) entrada;
            ListaEnlazada<EntradaSistemaArchivos> hijos = dir.getHijos();
            for (int i = 0; i < hijos.tamano(); i++) {
                eliminarRecursivo(hijos.obtener(i));
            }
        }
    }

    public ListaEnlazada<Archivo> getArchivos() {
        bloquear();
        try {
            ListaEnlazada<Archivo> lista = new ListaEnlazada<>();
            recolectarArchivos(raiz, lista);
            return lista;
        } finally {
            mutex.release();
        }
    }

    private void recolectarArchivos(Directorio dir, ListaEnlazada<Archivo> resultado) {
        ListaEnlazada<EntradaSistemaArchivos> hijos = dir.getHijos();
        for (int i = 0; i < hijos.tamano(); i++) {
            EntradaSistemaArchivos hijo = hijos.obtener(i);
            if (hijo instanceof Archivo) {
                resultado.agregar((Archivo) hijo);
            } else if (hijo instanceof Directorio) {
                recolectarArchivos((Directorio) hijo, resultado);
            }
        }
    }

    private Directorio obtenerDirectorioPadre(String rutaLimpia) {
        int ultimoSlash = rutaLimpia.lastIndexOf('/');
        String rutaPadre = (ultimoSlash <= 0) ? "/" : rutaLimpia.substring(0, ultimoSlash);
        EntradaSistemaArchivos padre = buscarInterno(rutaPadre);
        if (!(padre instanceof Directorio)) {
            throw new IllegalArgumentException("El directorio padre no existe: " + rutaPadre);
        }
        return (Directorio) padre;
    }

    private String obtenerNombreFinal(String rutaLimpia) {
        int ultimoSlash = rutaLimpia.lastIndexOf('/');
        return rutaLimpia.substring(ultimoSlash + 1);
    }

    private String normalizarRuta(String ruta) {
        String r = ruta.trim();
        if (!r.startsWith("/")) {
            r = "/" + r;
        }
        if (r.length() > 1 && r.endsWith("/")) {
            r = r.substring(0, r.length() - 1);
        }
        return r;
    }
}