package edu.unimet.avilaos.controlador;

import edu.unimet.avilaos.cluster.Cluster;
import edu.unimet.avilaos.cluster.Computador;
import edu.unimet.avilaos.gui.CreadorProcesos;
import edu.unimet.avilaos.gui.PanelControles;
import edu.unimet.avilaos.gui.SolicitudProceso;
import edu.unimet.avilaos.gui.VistaCrearProceso;
import edu.unimet.avilaos.gui.VistaSimulacion;
import edu.unimet.avilaos.kernel.PCB;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import javax.swing.Timer;

/**
 * Conecta la interfaz gráfica con el sistema operativo simulado.
 * Crea el Cluster, recibe las solicitudes de la Vista 1, maneja los controles
 * y refresca la Vista 2. La GUI no conoce al Cluster: solo este controlador.
 */
public class ControladorSimulacion implements CreadorProcesos {

    private static final int[] RAM_INICIAL = {256, 256};
    private static final TipoPolitica POLITICA_INICIAL = TipoPolitica.FCFS;
    private static final int QUANTUM_INICIAL = 3;
    private static final int DURACION_INICIAL_MS = 500;

    private final VistaCrearProceso vistaCrear;
    private final VistaSimulacion vistaSimulacion;
    private final PanelControles controles;

    private Cluster cluster;
    private boolean iniciado;

    public ControladorSimulacion(VistaCrearProceso vistaCrear, VistaSimulacion vistaSimulacion,
                                 PanelControles controles) {
        this.vistaCrear = vistaCrear;
        this.vistaSimulacion = vistaSimulacion;
        this.controles = controles;

        vistaCrear.setCreador(this);
        conectarControles();
        configurar(RAM_INICIAL, POLITICA_INICIAL, QUANTUM_INICIAL, DURACION_INICIAL_MS);

        // Refresca la vista desde el hilo de Swing, sin bloquear la simulación
        new Timer(200, e -> refrescar()).start();
    }

    /** Crea un clúster nuevo. Si había uno corriendo, lo detiene. */
    public final void configurar(int[] ramPorComputador, TipoPolitica politica, int quantum, int duracionMs) {
        if (cluster != null) {
            cluster.getReloj().detener();
        }
        cluster = new Cluster(ramPorComputador, politica, quantum, duracionMs, vistaSimulacion.getPanelLog());
        iniciado = false;
        vistaSimulacion.cargarCluster(cluster);
        vistaCrear.setCantidadComputadores(cluster.getCantidad());
        controles.setQuantum(quantum);
        controles.setDuracionCiclo(duracionMs);
        controles.getBotonIniciar().setEnabled(true);
        controles.getBotonPausar().setEnabled(false);
        controles.getBotonPausar().setText("Pausar");
        controles.setCiclo(0);
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
