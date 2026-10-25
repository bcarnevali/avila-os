package edu.unimet.avilaos.kernel;

import edu.unimet.avilaos.estructuras.Cola;
import edu.unimet.avilaos.estructuras.Comparador;
import edu.unimet.avilaos.hardware.MemoriaPrincipal;
import edu.unimet.avilaos.kernel.politicas.FabricaPoliticas;
import edu.unimet.avilaos.kernel.politicas.PoliticaPlanificacion;
import edu.unimet.avilaos.metricas.Metricas;
import edu.unimet.avilaos.modelo.enums.EstadoProceso;
import edu.unimet.avilaos.modelo.enums.MotivoBloqueo;
import edu.unimet.avilaos.modelo.enums.TipoPolitica;
import java.util.concurrent.Semaphore;

/**
 * Planificador de un computador. Maneja las colas de nuevos, listos, bloqueados
 * y terminados, y decide qué proceso se admite, cuál se ejecuta, cuándo se
 * expropia y cuándo termina.
 *
 * Uso esperado en cada ciclo de reloj (desde Computador):
 *   1. descontarDeadlines(enEjecucion, ciclo)  -> si devuelve true, la CPU queda libre
 *   2. enEjecucion = planificar(enEjecucion, ciclo)
 *   3. la CPU ejecuta una instrucción de enEjecucion
 *   4. según lo que pase: bloquear(...), terminar(...) o nada
 *
 * Todas las colas están protegidas con un Semaphore de Java porque el hilo del
 * reloj las modifica y el hilo de la interfaz las lee.
 */
public class Planificador {

    private final int idComputador;
    private final MemoriaPrincipal memoria;
    private final Metricas metricas;
    private final RegistroEventos log;
    private final Semaphore mutex = new Semaphore(1);

    private final Cola<PCB> nuevos = new Cola<>();
    private final Cola<PCB> listos = new Cola<>();
    private final Cola<PCB> bloqueados = new Cola<>();
    private final Cola<PCB> terminados = new Cola<>();

    private PoliticaPlanificacion politica;
    private Comparador<PCB> orden;
    private int quantum;

    public Planificador(int idComputador, MemoriaPrincipal memoria, Metricas metricas,
                        TipoPolitica politicaInicial, int quantum, RegistroEventos log) {
        if (memoria == null || metricas == null || politicaInicial == null) {
            throw new IllegalArgumentException("Memoria, métricas y política son obligatorias");
        }
        if (quantum <= 0) {
            throw new IllegalArgumentException("El quantum debe ser mayor que 0");
        }
        this.idComputador = idComputador;
        this.memoria = memoria;
        this.metricas = metricas;
        this.quantum = quantum;
        this.log = log != null ? log : (ciclo, mensaje) -> { };
        usarPolitica(FabricaPoliticas.crear(politicaInicial, quantum));
    }

    // ===================== Operaciones públicas =====================

    /** Un proceso nuevo llega al computador: entra a nuevos y se intenta admitir. */
    public void crearProceso(PCB pcb, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            pcb.setEstado(EstadoProceso.NUEVO);
            pcb.setIdComputador(idComputador);
            nuevos.encolar(pcb);
            registrar(ciclo, "Proceso " + pcb + " llega a la cola de nuevos");
            admitirNuevosSinLock(ciclo);
            if (pcb.getEstado() == EstadoProceso.NUEVO) {
                registrar(ciclo, "Proceso " + pcb + " espera en nuevos: no hay memoria suficiente");
            }
        } finally {
            mutex.release();
        }
    }

    /**
     * Decide qué proceso ejecuta en este ciclo. Si el actual agotó su quantum o
     * debe ser expropiado, vuelve a listos y se selecciona otro.
     * Devuelve el proceso que debe ejecutar, o null si la CPU queda ociosa.
     */
    public PCB planificar(PCB enEjecucion, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            if (enEjecucion != null) {
                if (quantumAgotado(enEjecucion)) {
                    registrar(ciclo, "Proceso " + enEjecucion + " agota su quantum de " + quantum + " ciclos");
                    insertarEnListosSinLock(enEjecucion, ciclo);
                    enEjecucion = null;
                } else if (!listos.estaVacia() && politica.debeExpropiar(enEjecucion, listos.verPrimero())) {
                    registrar(ciclo, "Proceso " + listos.verPrimero() + " expropia a " + enEjecucion);
                    insertarEnListosSinLock(enEjecucion, ciclo);
                    enEjecucion = null;
                }
            }
            if (enEjecucion == null) {
                enEjecucion = seleccionarSinLock(ciclo);
            }
            if (enEjecucion != null) {
                enEjecucion.sumarCicloQuantum();
            }
            return enEjecucion;
        } finally {
            mutex.release();
        }
    }

    /** El proceso en ejecución se bloquea (E/S, semáforo o latencia de red). */
    public void bloquear(PCB pcb, MotivoBloqueo motivo, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            pcb.marcarInicioBloqueo(ciclo, motivo);
            bloqueados.encolar(pcb);
            registrar(ciclo, "Proceso " + pcb + " se bloquea (" + motivo + ")");
        } finally {
            mutex.release();
        }
    }

    /** Ocurrió el evento que esperaba: sale de bloqueados y vuelve a listos. */
    public void desbloquear(PCB pcb, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            if (!bloqueados.eliminar(pcb)) {
                return;
            }
            MotivoBloqueo motivo = pcb.getMotivoBloqueo();
            long duracion = pcb.marcarFinBloqueo(ciclo);
            if (motivo == MotivoBloqueo.SEMAFORO_VACIOS
                    || motivo == MotivoBloqueo.SEMAFORO_LLENOS
                    || motivo == MotivoBloqueo.SEMAFORO_MUTEX) {
                metricas.registrarBloqueoSemaforo(duracion);
            }
            registrar(ciclo, "Proceso " + pcb + " se desbloquea tras " + duracion + " ciclos");
            insertarEnListosSinLock(pcb, ciclo);
        } finally {
            mutex.release();
        }
    }

    /** El proceso en ejecución terminó normalmente. Libera su memoria y revisa nuevos. */
    public void terminar(PCB pcb, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            terminarSinLock(pcb, ciclo, false);
            admitirNuevosSinLock(ciclo);
        } finally {
            mutex.release();
        }
    }

    /**
     * Resta un ciclo al deadline de todos los procesos del computador.
     * Los que llegan a 0 se terminan, estén donde estén.
     * Devuelve true si el que estaba en ejecución fue terminado (la CPU queda libre).
     */
    public boolean descontarDeadlines(PCB enEjecucion, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            boolean terminoElDeCpu = false;
            boolean algunoTermino = false;
            if (enEjecucion != null && enEjecucion.descontarDeadline()) {
                terminarSinLock(enEjecucion, ciclo, true);
                terminoElDeCpu = true;
                algunoTermino = true;
            }
            algunoTermino |= descontarEnCola(nuevos, ciclo);
            algunoTermino |= descontarEnCola(listos, ciclo);
            algunoTermino |= descontarEnCola(bloqueados, ciclo);
            if (algunoTermino) {
                admitirNuevosSinLock(ciclo);
            }
            return terminoElDeCpu;
        } finally {
            mutex.release();
        }
    }

    /** Cambia la política en tiempo de ejecución y reordena la cola de listos. */
    public void cambiarPolitica(TipoPolitica tipo, long ciclo) {
        mutex.acquireUninterruptibly();
        try {
            usarPolitica(FabricaPoliticas.crear(tipo, quantum));
            reordenarListosSinLock();
            registrar(ciclo, "Computador " + idComputador + " cambia su política a " + tipo);
        } finally {
            mutex.release();
        }
    }

    /** Cambia el quantum. Si la política activa es Round Robin, se aplica de inmediato. */
    public void cambiarQuantum(int nuevoQuantum, long ciclo) {
        if (nuevoQuantum <= 0) {
            throw new IllegalArgumentException("El quantum debe ser mayor que 0");
        }
        mutex.acquireUninterruptibly();
        try {
            quantum = nuevoQuantum;
            if (politica.getTipo() == TipoPolitica.ROUND_ROBIN) {
                usarPolitica(FabricaPoliticas.crear(TipoPolitica.ROUND_ROBIN, quantum));
            }
            registrar(ciclo, "Computador " + idComputador + " cambia el quantum a " + quantum);
        } finally {
            mutex.release();
        }
    }

    // ===================== Consultas para la interfaz =====================

    public PCB[] getNuevos() { return copiar(nuevos); }
    public PCB[] getListos() { return copiar(listos); }
    public PCB[] getBloqueados() { return copiar(bloqueados); }
    public PCB[] getTerminados() { return copiar(terminados); }

    public TipoPolitica getTipoPolitica() {
        mutex.acquireUninterruptibly();
        try {
            return politica.getTipo();
        } finally {
            mutex.release();
        }
    }

    public int getQuantum() {
        mutex.acquireUninterruptibly();
        try {
            return quantum;
        } finally {
            mutex.release();
        }
    }

    public int getIdComputador() {
        return idComputador;
    }

    /** true si queda algún proceso sin terminar en este computador. */
    public boolean hayProcesosPendientes() {
        mutex.acquireUninterruptibly();
        try {
            return !nuevos.estaVacia() || !listos.estaVacia() || !bloqueados.estaVacia();
        } finally {
            mutex.release();
        }
    }

    // ===================== Lógica interna (se llama con el mutex tomado) =====================

    private void usarPolitica(PoliticaPlanificacion nueva) {
        politica = nueva;
        orden = (a, b) -> nueva.comparar(a, b);
    }

    private boolean quantumAgotado(PCB pcb) {
        int q = politica.getQuantum();
        return q > 0 && pcb.getCiclosEnQuantum() >= q;
    }

    private void insertarEnListosSinLock(PCB pcb, long ciclo) {
        pcb.setEstado(EstadoProceso.LISTO);
        pcb.marcarEntradaListos(ciclo);
        listos.insertarOrdenado(pcb, orden);
    }

    private PCB seleccionarSinLock(long ciclo) {
        PCB siguiente = listos.desencolar();
        if (siguiente == null) {
            return null;
        }
        boolean primeraVez = siguiente.getCicloPrimeraEjecucion() < 0;
        siguiente.setEstado(EstadoProceso.EJECUCION);
        siguiente.marcarSalidaListos(ciclo);
        if (primeraVez) {
            metricas.registrarTiempoRespuesta(ciclo - siguiente.getCicloLlegada());
        }
        registrar(ciclo, "Computador " + idComputador + " selecciona el proceso " + siguiente);
        return siguiente;
    }

    /** Planificación a largo plazo: admite los nuevos que quepan en memoria, en orden de llegada. */
    private void admitirNuevosSinLock(long ciclo) {
        int n = nuevos.tamano();
        for (int i = 0; i < n; i++) {
            PCB pcb = nuevos.desencolar();
            if (memoria.reservar(pcb.getMemoriaRequerida())) {
                registrar(ciclo, "Proceso " + pcb + " es admitido y reserva " + pcb.getMemoriaRequerida() + " de memoria");
                insertarEnListosSinLock(pcb, ciclo);
            } else {
                nuevos.encolar(pcb);
            }
        }
    }

    private void terminarSinLock(PCB pcb, long ciclo, boolean porDeadline) {
        boolean teniaMemoria = pcb.getEstado() != EstadoProceso.NUEVO;
        pcb.setEstado(EstadoProceso.TERMINADO);
        pcb.setMotivoBloqueo(MotivoBloqueo.NINGUNO);
        if (teniaMemoria) {
            memoria.liberar(pcb.getMemoriaRequerida());
        }
        terminados.encolar(pcb);
        metricas.registrarTiempoEspera(pcb.getCiclosEsperaTotal());
        if (porDeadline) {
            metricas.registrarDeadlineVencido();
            registrar(ciclo, "Proceso " + pcb + " es terminado: su deadline llegó a 0");
        } else {
            metricas.registrarProcesoTerminado();
            registrar(ciclo, "Proceso " + pcb + " termina y libera " + pcb.getMemoriaRequerida() + " de memoria");
        }
    }

    /** Recorre la cola completa conservando el orden; termina los que vencen. */
    private boolean descontarEnCola(Cola<PCB> cola, long ciclo) {
        boolean alguno = false;
        int n = cola.tamano();
        for (int i = 0; i < n; i++) {
            PCB pcb = cola.desencolar();
            if (pcb.descontarDeadline()) {
                terminarSinLock(pcb, ciclo, true);
                alguno = true;
            } else {
                cola.encolar(pcb);
            }
        }
        return alguno;
    }

    private void reordenarListosSinLock() {
        Cola<PCB> auxiliar = new Cola<>();
        while (!listos.estaVacia()) {
            auxiliar.encolar(listos.desencolar());
        }
        while (!auxiliar.estaVacia()) {
            listos.insertarOrdenado(auxiliar.desencolar(), orden);
        }
    }

    /** Copia una cola en un arreglo sin alterarla, para que la interfaz la muestre. */
    private PCB[] copiar(Cola<PCB> cola) {
        mutex.acquireUninterruptibly();
        try {
            int n = cola.tamano();
            PCB[] copia = new PCB[n];
            for (int i = 0; i < n; i++) {
                PCB pcb = cola.desencolar();
                copia[i] = pcb;
                cola.encolar(pcb);
            }
            return copia;
        } finally {
            mutex.release();
        }
    }

    private void registrar(long ciclo, String mensaje) {
        log.registrar(ciclo, "[Computador " + idComputador + "] " + mensaje);
    }
}
