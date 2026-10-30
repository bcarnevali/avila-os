# ÁvilaOS

Simulador de un sistema operativo distribuido en Java. Proyecto 1 de Sistemas Operativos, UNIMET 2627-1.

**Equipo:** Bettina Carnevali, María Fernanda Ballesteros y Daniela Urdaneta.

**Qué hace**

- Clúster de 2 a 8 computadores sincronizados por un reloj global, cada uno con su CPU, memoria, E/S y planificador.
- Políticas FCFS, Round Robin, EDF y Prioridad apropiativa, cambiables en plena ejecución.
- Productores y consumidores con buffers acotados y semáforos, locales o remotos con latencia de red.
- Métricas por computador y del sistema, y configuración guardada en CSV.

**Cómo ejecutarlo**

1. Abrir el proyecto en NetBeans con JDK 25.
2. Darle Run (clase principal `edu.unimet.avilaos.AvilaOs`).

JFreeChart se descarga solo con Maven.

**Flujo de trabajo**

`main` es la versión entregada y `develop` la de integración. Cada tarea se hizo en su rama con Pull Request revisado por otra integrante, y las tareas y errores se registraron en Issues.
