# Plan de pruebas — Práctica 3: Broker de mensajes

---

## Índice

1. Entorno de pruebas
2. Pruebas de la versión básica
3. Pruebas de las versiones avanzadas
4. Pruebas de robustez
5. Resultados esperados resumidos

---

## 1. Entorno de pruebas

### Arranque del broker

```bash
cd Pr3/broker
python broker_server.py
```

El broker debe mostrar:
```
[server] Broker escuchando en 0.0.0.0:5555
```

### Ficheros involucrados

| Fichero | Rol |
|---|---|
| `broker_server.py` | Servidor broker (núcleo + TCP) |
| `cliente.py` | Librería cliente compartida |
| `productor.py` | Ejemplo de productor |
| `consumidor.py` | Ejemplo de consumidor |

---

## 2. Pruebas de la versión básica

---

### PT-01 — Declarar cola (idempotencia)

**Objetivo**: verificar que `declarar_cola` es idempotente — llamarla varias veces con el mismo nombre solo crea una cola.

**Pasos**:
1. Arrancar el broker.
2. Desde `productor.py` o manualmente, declarar la cola `"test"` tres veces seguidas.

**Resultado esperado en el broker**:
```
[broker] Cola creada: 'test'
[broker] Cola ya existe: 'test'
[broker] Cola ya existe: 'test'
```

**Resultado esperado en el cliente**:
```json
{"status": "ok"}
{"status": "ok"}
{"status": "ok"}
```

**Criterio de éxito**: la cola se crea una única vez; las invocaciones siguientes devuelven `ok` sin crear duplicados.

---

### PT-02 — Publicar en cola existente

**Objetivo**: verificar que un productor puede publicar mensajes en una cola declarada.

**Pasos**:
1. Arrancar el broker.
2. Declarar la cola `"test"`.
3. Publicar 5 mensajes en la cola `"test"` sin consumidores suscritos.

**Resultado esperado en el broker**:
```
[broker] Mensaje encolado en 'test': <uuid>
[broker] Mensaje encolado en 'test': <uuid>
...  (5 veces)
```

**Resultado esperado en el cliente**:
```json
{"status": "ok"}
```
por cada mensaje publicado.

**Criterio de éxito**: los 5 mensajes se encolan correctamente. El broker no lanza errores.

---

### PT-03 — Publicar en cola inexistente (descarte)

**Objetivo**: verificar que publicar en una cola no declarada descarta el mensaje y devuelve error.

**Pasos**:
1. Arrancar el broker (sin declarar ninguna cola).
2. Intentar publicar un mensaje en `"cola_no_declarada"`.

**Resultado esperado en el broker**:
```
[broker] Cola 'cola_no_declarada' no existe — mensaje descartado
```

**Resultado esperado en el cliente**:
```json
{"status": "error", "reason": "queue not found"}
```

**Criterio de éxito**: el mensaje se descarta, el broker no cae y devuelve error al productor.

---

### PT-04 — Consumir mensajes con callback

**Objetivo**: verificar que un consumidor suscrito recibe mensajes y el broker invoca el callback.

**Pasos**:
1. Arrancar el broker.
2. Arrancar `consumidor.py` (se suscribe a `"test"`).
3. Arrancar `productor.py` (publica 10 mensajes en `"test"`).

**Resultado esperado en el consumidor**:
```
Procesando mensaje: 'Mensaje 0'
Procesando mensaje: 'Mensaje 1'
...
```

**Resultado esperado en el broker**:
```
[broker] Entregado msg <uuid> → consumidor en 'test'
```

**Criterio de éxito**: el consumidor recibe y procesa todos los mensajes publicados.

---

### PT-05 — Mensajes pendientes entregados al suscribirse

**Objetivo**: verificar que los mensajes encolados antes de que haya consumidor se entregan cuando uno se suscribe.

**Pasos**:
1. Arrancar el broker.
2. Arrancar `productor.py` — publica 5 mensajes (sin consumidores activos).
3. Comprobar que los mensajes quedan encolados en el broker.
4. Arrancar `consumidor.py`.

**Resultado esperado**: el consumidor recibe los 5 mensajes encolados inmediatamente tras suscribirse.

**Criterio de éxito**: no se pierde ningún mensaje publicado antes de la suscripción.

---

### PT-06 — Round robin entre múltiples consumidores

**Objetivo**: verificar que los mensajes se reparten equitativamente entre consumidores.

**Pasos**:
1. Arrancar el broker.
2. Arrancar dos instancias de `consumidor.py` en terminales distintas (ambas suscritas a `"test"`).
3. Publicar 10 mensajes con el `productor.py`.

**Resultado esperado**: cada consumidor recibe exactamente 5 mensajes (alternando: C1→C2→C1→C2...).

**Criterio de éxito**: distribución equitativa entre consumidores. Ninguno recibe todos los mensajes.

---

### PT-07 — Expiración de mensajes (TTL 5 minutos)

**Objetivo**: verificar que los mensajes sin consumidor se eliminan tras 5 minutos.

**Pasos**:
1. Arrancar el broker.
2. Modificar temporalmente `Message.TTL = 10` (10 segundos para acelerar la prueba).
3. Publicar 3 mensajes en `"test"` sin consumidores.
4. Esperar 60 segundos (ciclo del hilo de expiración).
5. Suscribir un consumidor.

**Resultado esperado en el broker**:
```
[broker] Mensaje encolado en 'test': <uuid>   (x3)
```
Tras 60s, los mensajes son eliminados. El consumidor no recibe ninguno.

**Criterio de éxito**: los mensajes caducados no se entregan. La cola queda vacía.

---

## 3. Pruebas de las versiones avanzadas

---

### PT-08 — ACK: confirmación de mensaje procesado

**Objetivo**: verificar que el broker elimina el mensaje de `unacked` al recibir el ACK.

**Pasos**:
1. Arrancar el broker.
2. Arrancar el consumidor con ACK habilitado.
3. Publicar un mensaje.
4. Verificar en los logs del broker que se recibe el ACK.

**Resultado esperado en el broker**:
```
[broker] Entregado msg <uuid> → consumidor en 'test'
[broker] ACK recibido para msg <uuid> en 'test'
```

**Criterio de éxito**: el mensaje desaparece de `unacked` tras el ACK. No se reencola.

---

### PT-09 — ACK: reencola si el consumidor cae sin confirmar

**Objetivo**: verificar que si el consumidor se cae antes de enviar ACK, el mensaje se reencola.

**Pasos**:
1. Arrancar el broker.
2. Arrancar un consumidor con `time.sleep(30)` en el callback (simula procesamiento lento).
3. Publicar un mensaje — el consumidor lo recibe pero aún no envía ACK.
4. Matar el consumidor antes de que termine el sleep (Ctrl+C).
5. Arrancar un segundo consumidor.

**Resultado esperado en el broker**:
```
[broker] Reencolado msg <uuid> tras desconexión
[broker] Entregado msg <uuid> → consumidor en 'test'
```

**Criterio de éxito**: el mensaje se reencola y es procesado por el segundo consumidor. No se pierde.

---

### PT-10 — Fair dispatch: no enviar a consumidor ocupado

**Objetivo**: verificar que con fair dispatch el broker no envía un nuevo mensaje a un consumidor que tiene uno pendiente de ACK.

**Pasos**:
1. Arrancar el broker.
2. Arrancar un consumidor con `time.sleep(3)` en el callback.
3. Publicar 3 mensajes rápidamente (cada segundo).

**Resultado esperado en el broker**:
```
[broker] Entregado msg <uuid1> → consumidor en 'test'
[broker] Mensaje encolado en 'test': <uuid2>      ← consumidor ocupado
[broker] ACK recibido para msg <uuid1> en 'test'
[broker] Entregado msg <uuid2> → consumidor en 'test'
```

**Criterio de éxito**: mientras el consumidor está procesando, los nuevos mensajes se encolan en vez de enviarse.

---

### PT-11 — Listar colas

**Objetivo**: verificar que `listar_colas` devuelve todas las colas declaradas.

**Pasos**:
1. Arrancar el broker.
2. Declarar las colas `"test"`, `"pedidos"` y `"notificaciones"`.
3. Llamar a `cliente.listar_colas()`.

**Resultado esperado**:
```json
{"status": "ok", "queues": ["test", "pedidos", "notificaciones"]}
```

**Criterio de éxito**: la respuesta contiene exactamente las tres colas declaradas.

---

### PT-12 — Eliminar cola

**Objetivo**: verificar que `eliminar_cola` borra la cola y sus mensajes pendientes.

**Pasos**:
1. Arrancar el broker.
2. Declarar `"test"` y publicar 3 mensajes.
3. Eliminar la cola `"test"`.
4. Intentar publicar en `"test"`.

**Resultado esperado**:
- La eliminación devuelve `{"status": "ok"}`.
- La publicación posterior devuelve `{"status": "error", "reason": "queue not found"}`.

**Criterio de éxito**: la cola desaparece completamente. Los mensajes pendientes se pierden (comportamiento esperado).

---

### PT-13 — Durabilidad: recuperación tras caída del broker

**Objetivo**: verificar que los mensajes pendientes y las colas se restauran al reiniciar el broker.

**Pasos**:
1. Arrancar el broker.
2. Declarar `"test"` y publicar 5 mensajes sin consumidores.
3. Parar el broker (Ctrl+C).
4. Comprobar que existe `broker_state.json` con los mensajes.
5. Reiniciar el broker.
6. Arrancar un consumidor.

**Resultado esperado en el broker al reiniciar**:
```
[broker] Cola 'test' restaurada (5 msgs)
[server] Broker escuchando en 0.0.0.0:5555
```

**Resultado esperado en el consumidor**: recibe los 5 mensajes publicados antes de la caída.

**Criterio de éxito**: ningún mensaje se pierde tras reiniciar el broker.

---

### PT-14 — Ejecución en máquinas distintas

**Objetivo**: verificar que productores y consumidores pueden conectarse al broker desde otra IP.

**Pasos**:
1. Arrancar el broker en la máquina A con `host="0.0.0.0"`.
2. Obtener la IP de la máquina A (por ejemplo `192.168.0.160`).
3. Desde la máquina B, modificar `cliente.py` para usar `host="192.168.0.160"`.
4. Ejecutar `productor.py` y `consumidor.py` desde la máquina B.

**Alternativa sin segunda máquina**: usar la IP local de red (`192.168.0.160`) en vez de `localhost` en el mismo PC.

**Resultado esperado**: el sistema funciona exactamente igual que en local.

**Criterio de éxito**: mensajes publicados desde la máquina B son recibidos por el consumidor en la máquina B, pasando por el broker en la máquina A.

---

## 4. Pruebas de robustez

---

### PT-15 — Múltiples productores simultáneos

**Objetivo**: verificar que el broker gestiona correctamente mensajes de varios productores a la vez.

**Pasos**:
1. Arrancar el broker y un consumidor.
2. Lanzar 3 instancias de `productor.py` en paralelo, cada una publicando 5 mensajes.

**Resultado esperado**: el consumidor recibe los 15 mensajes en total, sin pérdidas ni errores.

**Criterio de éxito**: no hay mensajes duplicados ni perdidos. El broker no cae.

---

### PT-16 — Desconexión y reconexión del consumidor

**Objetivo**: verificar que el broker detecta la desconexión y un nuevo consumidor puede suscribirse.

**Pasos**:
1. Arrancar el broker y un consumidor.
2. Matar el consumidor (Ctrl+C).
3. Publicar 3 mensajes.
4. Arrancar un nuevo consumidor.

**Resultado esperado**: el nuevo consumidor recibe los 3 mensajes encolados durante su ausencia.

**Criterio de éxito**: el broker elimina el consumidor desconectado de la lista y los mensajes se entregan al nuevo.

---

### PT-17 — Parada limpia del broker

**Objetivo**: verificar que al parar el broker con Ctrl+C se guarda el estado correctamente.

**Pasos**:
1. Arrancar el broker.
2. Publicar mensajes y dejar algunos sin consumir.
3. Parar el broker con Ctrl+C.

**Resultado esperado en el broker**:
```
[server] Deteniendo broker (Ctrl+C detectado)...
[broker] Estado guardado (N colas)
```

**Criterio de éxito**: el fichero `broker_state.json` se actualiza antes de que el proceso termine.

---

## 5. Resultados esperados resumidos

| ID | Descripción | Versión | Criterio |
|---|---|---|---|
| PT-01 | Declarar cola idempotente | Básica | Solo se crea una vez |
| PT-02 | Publicar en cola existente | Básica | Mensajes encolados |
| PT-03 | Publicar en cola inexistente | Básica | Mensaje descartado, error devuelto |
| PT-04 | Consumir con callback | Básica | Todos los mensajes procesados |
| PT-05 | Mensajes pendientes al suscribirse | Básica | No se pierden mensajes previos |
| PT-06 | Round robin entre consumidores | Básica | Distribución equitativa |
| PT-07 | Expiración TTL 5 minutos | Básica | Mensajes caducados eliminados |
| PT-08 | ACK confirmado | Avanzada | Mensaje eliminado de unacked |
| PT-09 | Reencola sin ACK | Avanzada | Mensaje no se pierde |
| PT-10 | Fair dispatch | Avanzada | No envío a consumidor ocupado |
| PT-11 | Listar colas | Avanzada | Devuelve lista correcta |
| PT-12 | Eliminar cola | Avanzada | Cola y mensajes borrados |
| PT-13 | Durabilidad tras caída | Avanzada | Mensajes restaurados |
| PT-14 | Ejecución en máquinas distintas | Avanzada | Funciona por red |
| PT-15 | Múltiples productores | Robustez | Sin pérdidas ni duplicados |
| PT-16 | Desconexión y reconexión | Robustez | Nuevo consumidor recibe mensajes |
| PT-17 | Parada limpia | Robustez | Estado guardado correctamente |
