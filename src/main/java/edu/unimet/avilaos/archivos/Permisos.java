package edu.unimet.avilaos.archivos;

import edu.unimet.avilaos.modelo.enums.ModoUsuario;
import edu.unimet.avilaos.modelo.enums.TipoPermiso;

public final class Permisos {

    private Permisos() {
    }

    public static String motivoParaLeer(ModoUsuario modo, String usuario, EntradaSistemaArchivos entrada) {
        if (entrada == null) {
            return "El archivo o directorio no existe.";
        }
        if (modo == ModoUsuario.ADMINISTRADOR) {
            return null;
        }
        if (entrada.getPermiso() == TipoPermiso.PUBLICO) {
            return null;
        }
        if (usuario != null && usuario.equals(entrada.getDueno())) {
            return null;
        }
        return "Permiso denegado: '" + entrada.getNombre() + "' es privado y pertenece a " + entrada.getDueno() + ".";
    }

    public static String motivoParaCrear(ModoUsuario modo, String usuario, Directorio directorioPadre) {
        if (directorioPadre == null) {
            return "El directorio destino no existe.";
        }
        if (modo == ModoUsuario.ADMINISTRADOR) {
            return null;
        }
        if (usuario != null && usuario.equals(directorioPadre.getDueno())) {
            return null;
        }
        return "Permiso denegado: solo puedes crear archivos dentro de tus propios directorios.";
    }
    
    public static String motivoParaModificar(ModoUsuario modo, String usuario, EntradaSistemaArchivos entrada) {
        if (entrada == null) {
            return "El elemento no existe.";
        }
        if (modo == ModoUsuario.ADMINISTRADOR) {
            return null;
        }
        if (usuario != null && usuario.equals(entrada.getDueno())) {
            return null;
        }
        return "Permiso denegado: solo el administrador o el dueño (" + entrada.getDueno() + ") puede modificar '" + entrada.getNombre() + "'.";
    }
    

    public static String motivoParaEliminar(ModoUsuario modo, String usuario, EntradaSistemaArchivos entrada) {
        if (entrada == null) {
            return "El elemento no existe.";
        }
        if (entrada.getPadre() == null) {
            return "No se puede eliminar el directorio raíz '/'.";
        }
        if (modo == ModoUsuario.ADMINISTRADOR) {
            return null;
        }
        if (entrada instanceof Directorio) {
            return "Permiso denegado: un usuario estándar no puede eliminar directorios.";
        }
        if (usuario != null && usuario.equals(entrada.getDueno())) {
            return null;
        }
        return "Permiso denegado: solo puedes eliminar tus propios archivos.";
    }

    public static String motivoParaRenombrar(ModoUsuario modo, String usuario, EntradaSistemaArchivos entrada) {
        if (entrada == null) {
            return "El elemento no existe.";
        }
        if (entrada.getPadre() == null) {
            return "No se puede renombrar el directorio raíz '/'.";
        }
        if (modo == ModoUsuario.ADMINISTRADOR) {
            return null;
        }
        return "Permiso denegado: solo el administrador puede renombrar archivos o directorios.";
    }
}