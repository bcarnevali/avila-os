package edu.unimet.avilaos.controlador;

import edu.unimet.avilaos.cluster.Cluster;
import edu.unimet.avilaos.cluster.Computador;
import edu.unimet.avilaos.configuracion.BufferInicial;
import edu.unimet.avilaos.configuracion.ConfiguracionSimulacion;
import edu.unimet.avilaos.configuracion.GestorConfiguracion;
import edu.unimet.avilaos.configuracion.ReceptorConfiguracion;
import edu.unimet.avilaos.estructuras.ListaEnlazada;
import edu.unimet.avilaos.gui.CreadorBuffers;
import edu.unimet.avilaos.gui.CreadorProcesos;
import edu.unimet.avilaos.gui.PanelControles;
import edu.unimet.avilaos.gui.SolicitudProceso;
import edu.unimet.avilaos.gui.VistaConfiguracion;
import edu.unimet.avilaos.gui.VistaCrearProceso;
import edu.unimet.avilaos.gui.VistaSimulacion;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import edu.unimet.avilaos.modelo.enums.TipoProceso;
import javax.swing.Timer;

/**
 * Conecta la interfaz gráfica con el sistema operativo simulado.
 * Crea el Cluster, recibe las solicitudes de la Vista 1, maneja los controles
 * y refresca la Vista 2. La GUI no conoce al Cluster: solo este controlador.
 */
public class ControladorSimulacion implements CreadorProcesos, CreadorBuffers, ReceptorConfiguracion {

    private final VistaCrearProceso vistaCrear;
    private final VistaSimulacion vistaSimulacion;
    private final PanelControles controles;

    private Cluster cluster;
    private boolean iniciado;
    private ConfiguracionSimulacion configuracionActual;
    /** Lo que se ha creado en esta simulacion, para poder guardarlo como carga inicial. */
    private ListaEnlazada<SolicitudProceso> procesosCreados = new ListaEnlazada<>();
    private ListaEnlazada<BufferInicial> buffersCreados = new ListaEnlazada<>();

    public ControladorSimulacion(VistaConfiguracion vistaConfiguracion, VistaCrearProceso vistaCrear,
                                 VistaSimulacion vistaSimulacion, PanelControles controles) {
        this.vistaCrear = vistaCrear;
        this.vistaSimulacion = vistaSimulacion;
        this.controles = controles;

        vistaCrear.setCreador(this);
        vistaCrear.getPanelCrearBuffer().setCreador(this);
        conectarControles();

        // Si hay un archivo de configuracion guardado, la simulacion arranca con el
        ConfiguracionSimulacion inicial = ConfiguracionSimulacion.porDefecto();
        String aviso = null;
        if (GestorConfiguracion.ARCHIVO_POR_DEFECTO.exists()) {
            try {
                inicial = GestorConfiguracion.cargar(GestorConfiguracion.ARCHIVO_POR_DEFECTO);
                aviso = "[ÁvilaOS] Configuración cargada de " + GestorConfiguracion.ARCHIVO_POR_DEFECTO.getName();
            } catch (Exception ex) {
                aviso = "[ÁvilaOS] No se pudo leer " + GestorConfiguracion.ARCHIVO_POR_DEFECTO.getName()
                        + " (" + ex.getMessage() + "). Se usa la configuración por defecto.";
            }
        }
        aplicarConfiguracion(inicial);
        if (aviso != null) {
            vistaSimulacion.getPanelLog().registrar(0, aviso);
        }
        vistaConfiguracion.setReceptor(this);

        // Refresca la vista desde el hilo de Swing, sin bloquear la simulación
        new Timer(200, e -> refrescar()).start();
    }

    // ===================== Configuración =====================

    /**
     * Crea un clúster nuevo con la configuración (si había uno corriendo, lo detiene)
     * y crea la carga inicial de buffers y procesos.
     */
    @Override
    public final void aplicarConfiguracion(ConfiguracionSimulacion c) {
        c.validar();
        if (cluster != null) {
            cluster.getReloj().detener();
        }
        cluster = new Cluster(c.getRamPorComputador(), c.getPoliticas(), c.getQuantum(),
                c.getDuracionCicloMs(), c.getLatencia(), vistaSimulacion.getPanelLog());
        configuracionActual = c;
        procesosCreados = new ListaEnlazada<>();
        buffersCreados = new ListaEnlazada<>();
        iniciado = false;

        vistaSimulacion.cargarCluster(cluster);
        vistaCrear.setCantidadComputadores(cluster.getCantidad());
        controles.setQuantum(c.getQuantum());
        controles.setDuracionCiclo(c.getDuracionCicloMs());
        controles.setLatencia(c.getLatencia());
        controles.getBotonIniciar().setEnabled(true);
        controles.getBotonPausar().setEnabled(false);
        controles.getBotonPausar().setText("Pausar");
        controles.setCiclo(0);

        // Primero los buffers, porque los productores y consumidores los necesitan
        for (int i = 0; i < c.getBuffers().tamano(); i++) {
            BufferInicial b = c.getBuffers().obtener(i);
            crearBuffer(b.id(), b.capacidad(), b.idAnfitrion());
        }
        for (int i = 0; i < c.getProcesos().tamano(); i++) {
            crearProceso(c.getProcesos().obtener(i));
        }
    }

    @Override
    public ConfiguracionSimulacion getConfiguracionActual() {
        return configuracionActual;
    }

    @Override
    public ConfiguracionSimulacion capturarCargaActual() {
        ConfiguracionSimulacion carga = new ConfiguracionSimulacion();
        carga.setProcesos(procesosCreados);
        carga.setBuffers(buffersCreados);
        return carga;
    }

    // ===================== Vista 1: crear procesos =====================

    /** La Vista 1 ya validó los datos; aquí se crea el PCB y se envía al computador. */
    @Override
    public void crearProceso(SolicitudProceso s) {
        boolean automatico = s.idComputador() == SolicitudProceso.AUTOMATICO;
        Computador destino = automatico ? elegirAutomatico() : buscarComputador(s.idComputador());

        int capacidad = destino.getMemoria().getCapacidad();
        if (s.memoria() > capacidad) {
            throw new IllegalArgumentException("El proceso necesita " + s.memoria()
                    + " de memoria y el Computador " + destino.getId() + " solo tiene " + capacidad
                    + ". Nunca podría ser admitido.");
        }

        boolean usaBuffer = s.tipo() == TipoProceso.PRODUCTOR || s.tipo() == TipoProceso.CONSUMIDOR;
        if (usaBuffer && cluster.getBuffer(s.idBuffer()) == null) {
            throw new IllegalArgumentException("El buffer \"" + s.idBuffer()
                    + "\" no existe. Créalo primero en el panel \"Crear buffer\".");
        }

        long ciclo = cluster.getReloj().getCicloActual();
        PCB pcb = new PCB(s.nombre(), s.tipo(), s.prioridad(), s.memoria(),
                s.instrucciones(), s.deadline(), destino.getId(), ciclo);
        if (pcb.esProductorOConsumidor()) {
            pcb.configurarBuffer(s.idBuffer(), s.ciclosPorElemento(), s.elementosRequeridos());
        } else {
            pcb.configurarES(s.ciclosEntreES(), s.ciclosParaSatisfacerES());
        }

        if (automatico) {
            vistaSimulacion.getPanelLog().registrar(ciclo, "[ÁvilaOS] Proceso " + pcb
                    + " asignado automáticamente al Computador " + destino.getId() + " (el de más memoria libre)");
        }
        destino.crearProceso(pcb, ciclo);
        procesosCreados.agregar(s);
    }

    // ===================== Buffers =====================

    /** Crea un buffer en el computador anfitrión. Su capacidad ocupa RAM de ese computador. */
    @Override
    public void crearBuffer(String id, int capacidad, int idAnfitrion) {
        if (cluster.getBuffer(id) != null) {
            throw new IllegalArgumentException("Ya existe un buffer llamado \"" + id + "\".");
        }
        Computador anfitrion = buscarComputador(idAnfitrion);
        if (!anfitrion.getMemoria().cabe(capacidad)) {
            throw new IllegalArgumentException("El Computador " + idAnfitrion + " solo tiene "
                    + anfitrion.getMemoria().getLibre() + " de memoria libre y el buffer necesita " + capacidad + ".");
        }
        cluster.crearBuffer(id, capacidad, idAnfitrion);
        buffersCreados.agregar(new BufferInicial(id, capacidad, idAnfitrion));
        vistaSimulacion.getPanelLog().registrar(cluster.getReloj().getCicloActual(), "[ÁvilaOS] Se crea el buffer "
                + id + " con capacidad " + capacidad + " en el Computador " + idAnfitrion);
    }

    /** Balanceo de carga: elige el computador con más memoria libre (en empate, el de menor número). */
    private Computador elegirAutomatico() {
        Computador mejor = cluster.getComputador(1);
        for (int id = 2; id <= cluster.getCantidad(); id++) {
            Computador c = cluster.getComputador(id);
            if (c.getMemoria().getLibre() > mejor.getMemoria().getLibre()) {
                mejor = c;
            }
        }
        return mejor;
    }

    private Computador buscarComputador(int id) {
        if (id < 1 || id > cluster.getCantidad()) {
            throw new IllegalArgumentException("El computador " + id + " no existe.");
        }
        return cluster.getComputador(id);
    }

    // ===================== Controles =====================

    private void conectarControles() {
        controles.getBotonIniciar().addActionListener(e -> iniciar());
        controles.getBotonPausar().addActionListener(e -> pausarOReanudar());
        controles.getBotonAplicar().addActionListener(e -> aplicarParametros());
    }

    private void iniciar() {
        if (iniciado) {
            return;
        }
        cluster.iniciar();
        iniciado = true;
        controles.getBotonIniciar().setEnabled(false);
        controles.getBotonPausar().setEnabled(true);
    }

    private void pausarOReanudar() {
        if (cluster.getReloj().isPausado()) {
            cluster.getReloj().reanudar();
            controles.getBotonPausar().setText("Pausar");
        } else {
            cluster.getReloj().pausar();
            controles.getBotonPausar().setText("Reanudar");
        }
    }

    /** Cambia en ejecución la duración del ciclo, el quantum y la latencia de red. Solo registra lo que cambió. */
    private void aplicarParametros() {
        cluster.getReloj().setDuracionCiclo(controles.getDuracionCiclo());
        long ciclo = cluster.getReloj().getCicloActual();
        for (int id = 1; id <= cluster.getCantidad(); id++) {
            if (cluster.getComputador(id).getPlanificador().getQuantum() != controles.getQuantum()) {
                cluster.getComputador(id).getPlanificador().cambiarQuantum(controles.getQuantum(), ciclo);
            }
        }
        if (cluster.getRed().getLatencia() != controles.getLatencia()) {
            cluster.getRed().setLatencia(controles.getLatencia());
            vistaSimulacion.getPanelLog().registrar(ciclo, "[ÁvilaOS] La latencia de red cambia a "
                    + controles.getLatencia() + " ciclos");
        }
    }

    private void refrescar() {
        vistaSimulacion.actualizarDatos(cluster);
        controles.setCiclo(cluster.getReloj().getCicloActual());
    }

    public Cluster getCluster() {
        return cluster;
    }
}
