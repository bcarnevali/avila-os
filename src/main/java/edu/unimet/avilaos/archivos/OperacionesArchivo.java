package edu.unimet.avilaos.archivos;

import edu.unimet.avilaos.disco.Disco;
import edu.unimet.avilaos.disco.SolicitudES;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.kernel.ServicioArchivos;
import edu.unimet.avilaos.modelo.enums.ModoUsuario;
import edu.unimet.avilaos.modelo.enums.OperacionES;
import edu.unimet.avilaos.modelo.enums.TipoPermiso;

public class OperacionesArchivo implements ServicioArchivos {

    private final SistemaArchivos fs;
    private final Disco disco;

    public OperacionesArchivo(SistemaArchivos fs, Disco disco) {
        if (fs == null || disco == null) {
            throw new IllegalArgumentException("El sistema de archivos y el disco son obligatorios.");
        }
        this.fs = fs;
        this.disco = disco;
    }

    @Override
    public SolicitudES prepararSolicitud(PCB pcb, long ciclo) {
        if (pcb == null) {
            throw new IllegalStateException("El proceso no puede ser nulo.");
        }
        OperacionES operacion = pcb.getOperacionArchivo();
        String ruta = pcb.getRutaArchivo();
        if (operacion == null || ruta == null) {
            throw new IllegalStateException("El proceso no tiene configurada una operación o ruta.");
        }

        ModoUsuario modo = pcb.isAdministrador() ? ModoUsuario.ADMINISTRADOR : ModoUsuario.USUARIO;
        String usuario = pcb.getUsuario();

        switch (operacion) {
            case LEER: {
                EntradaSistemaArchivos entrada = fs.buscar(ruta);
                if (entrada == null) {
                    throw new IllegalStateException("La ruta '" + ruta + "' no existe.");
                }
                String motivo = Permisos.motivoParaLeer(modo, usuario, entrada);
                if (motivo != null) {
                    throw new IllegalStateException(motivo);
                }
                if (entrada instanceof Archivo) {
                    Archivo a = (Archivo) entrada;
                    return new SolicitudES(pcb, OperacionES.LEER, a.getPrimerBloque(), a.getTamanoBloques(), ciclo);
                }
                return new SolicitudES(pcb, OperacionES.LEER, 0, 1, ciclo);
            }

            case ACTUALIZAR: {
                EntradaSistemaArchivos entrada = fs.buscar(ruta);
                if (entrada == null) {
                    throw new IllegalStateException("La ruta '" + ruta + "' no existe.");
                }
                String nuevoNombre = pcb.getNuevoNombre();
                String motivo = (nuevoNombre != null && !nuevoNombre.isBlank())
                        ? Permisos.motivoParaRenombrar(modo, usuario, entrada)
                        : Permisos.motivoParaModificar(modo, usuario, entrada);
                if (motivo != null) {
                    throw new IllegalStateException(motivo);
                }
                if (entrada instanceof Archivo) {
                    Archivo a = (Archivo) entrada;
                    return new SolicitudES(pcb, OperacionES.ACTUALIZAR, a.getPrimerBloque(), a.getTamanoBloques(), ciclo);
                }
                return new SolicitudES(pcb, OperacionES.ACTUALIZAR, 0, 1, ciclo);
            }

            case ELIMINAR: {
                EntradaSistemaArchivos entrada = fs.buscar(ruta);
                if (entrada == null) {
                    throw new IllegalStateException("La ruta '" + ruta + "' no existe.");
                }
                String motivo = Permisos.motivoParaEliminar(modo, usuario, entrada);
                if (motivo != null) {
                    throw new IllegalStateException(motivo);
                }
                if (entrada instanceof Archivo) {
                    Archivo a = (Archivo) entrada;
                    return new SolicitudES(pcb, OperacionES.ELIMINAR, a.getPrimerBloque(), 1, ciclo);
                }
                return new SolicitudES(pcb, OperacionES.ELIMINAR, 0, 1, ciclo);
            }

            case CREAR: {
                String limpia = normalizarRuta(ruta);
                if ("/".equals(limpia)) {
                    throw new IllegalStateException("No se puede crear la raíz nuevamente.");
                }
                if (fs.buscar(limpia) != null) {
                    throw new IllegalStateException("Ya existe un elemento en '" + limpia + "'.");
                }
                Directorio padre = buscarPadre(limpia);
                if (padre == null) {
                    throw new IllegalStateException("El directorio destino no existe.");
                }
                String motivo = Permisos.motivoParaCrear(modo, usuario, padre);
                if (motivo != null) {
                    throw new IllegalStateException(motivo);
                }

                int bloques = pcb.getBloquesArchivo();
                if (bloques == 0) {
                    return new SolicitudES(pcb, OperacionES.CREAR, 0, 1, ciclo);
                }

                if (disco.getBloquesDisponiblesParaArchivos() < bloques) {
                    throw new IllegalStateException("No hay espacio en el disco.");
                }
                TipoPermiso permiso = (modo == ModoUsuario.ADMINISTRADOR) ? TipoPermiso.PUBLICO : TipoPermiso.PRIVADO;
                try {
                    Archivo creado = fs.crearArchivo(limpia, bloques, usuario, permiso, pcb.getNombre());
                    return new SolicitudES(pcb, OperacionES.CREAR, creado.getPrimerBloque(), bloques, ciclo);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }

            default:
                throw new IllegalStateException("Operación no soportada: " + operacion);
        }
    }

    @Override
    public void completarOperacion(PCB pcb, SolicitudES solicitud, long ciclo) {
        if (pcb == null) {
            throw new IllegalStateException("El proceso no puede ser nulo.");
        }
        OperacionES operacion = pcb.getOperacionArchivo();
        String ruta = pcb.getRutaArchivo();
        ModoUsuario modo = pcb.isAdministrador() ? ModoUsuario.ADMINISTRADOR : ModoUsuario.USUARIO;
        String usuario = pcb.getUsuario();

        try {
            switch (operacion) {
                case CREAR:
                    if (pcb.getBloquesArchivo() == 0) {
                        TipoPermiso permiso = (modo == ModoUsuario.ADMINISTRADOR) ? TipoPermiso.PUBLICO : TipoPermiso.PRIVADO;
                        fs.crearDirectorio(ruta, usuario, permiso);
                    }
                    break;

                case ELIMINAR:
                    fs.eliminar(ruta);
                    break;

                case ACTUALIZAR:
                    String nuevoNombre = pcb.getNuevoNombre();
                    if (nuevoNombre != null && !nuevoNombre.isBlank()) {
                        fs.renombrar(ruta, nuevoNombre);
                    }
                    break;

                case LEER:
                default:
                    break;
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private Directorio buscarPadre(String rutaLimpia) {
        int ultimoSlash = rutaLimpia.lastIndexOf('/');
        String rutaPadre = (ultimoSlash <= 0) ? "/" : rutaLimpia.substring(0, ultimoSlash);
        EntradaSistemaArchivos entrada = fs.buscar(rutaPadre);
        return (entrada instanceof Directorio) ? (Directorio) entrada : null;
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