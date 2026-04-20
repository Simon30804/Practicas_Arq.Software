# Ejecución en máquinas del laboratorio — Práctica 3 (Python)

A diferencia de la práctica anterior en Java con RMI, esta práctica usa
sockets TCP puros. No necesitas rmiregistry ni compilar nada.

---

## Distribución de máquinas

| Máquina | IP | Rol |
|---|---|---|
| lab102-196 | 155.210.154.196 | Broker de mensajes |
| lab102-197 | 155.210.154.197 | Productor |
| lab102-198 | 155.210.154.198 | Consumidor |

---

## Paso 1 — Encender las máquinas remotas

Conéctate a la máquina central de la universidad:

```bash
ssh a869800@central.cps.unizar.es
```

Encender las tres máquinas:

```bash
/usr/local/etc/wake -y lab102-196
/usr/local/etc/wake -y lab102-197
/usr/local/etc/wake -y lab102-198
```

Verificar que responden:

```bash
ping 155.210.154.196
ping 155.210.154.197
ping 155.210.154.198
```

---

## Paso 2 — Copiar los ficheros a cada máquina

Desde tu máquina local, copia los ficheros de la práctica:

```bash
# Crear directorio en cada máquina remota
ssh a869800@155.210.154.196 "mkdir -p ~/practica3"
ssh a869800@155.210.154.197 "mkdir -p ~/practica3"
ssh a869800@155.210.154.198 "mkdir -p ~/practica3"

# Copiar broker a la máquina 196
scp broker_server.py cliente.py a869800@155.210.154.196:~/practica3/

# Copiar productor a la máquina 197
scp cliente.py productor.py a869800@155.210.154.197:~/practica3/

# Copiar consumidor a la máquina 198
scp cliente.py consumidor.py a869800@155.210.154.198:~/practica3/
```

---

## Paso 3 — Arrancar el broker (máquina 196)

```bash
ssh a869800@155.210.154.196
cd ~/practica3
python3 broker_server.py
```

Debes ver:
```
[server] Broker escuchando en 0.0.0.0:5555
```

Mantén esta sesión SSH abierta.

---

## Paso 4 — Arrancar el consumidor (máquina 198)

Abre otra terminal y conéctate a la máquina 198.
Antes de ejecutar, edita `consumidor.py` para apuntar al broker:

```bash
ssh a869800@155.210.154.198
cd ~/practica3
```

Edita la línea de conexión en `consumidor.py`:

```python
# Cambiar localhost por la IP del broker
cliente = Cliente(host="155.210.154.196", port=5555)
```

Lanza el consumidor:

```bash
python3 consumidor.py
```

---

## Paso 5 — Arrancar el productor (máquina 197)

Abre otra terminal y conéctate a la máquina 197.
Edita `productor.py` para apuntar al broker:

```bash
ssh a869800@155.210.154.197
cd ~/practica3
```

Edita la línea de conexión en `productor.py`:

```python
# Cambiar localhost por la IP del broker
cliente = Cliente(host="155.210.154.196", port=5555)
```

Lanza el productor:

```bash
python3 productor.py
```

---

## Resultado esperado

**Broker (196)**:
```
[broker] Cliente conectado: ('155.210.154.198', XXXXX)
[broker] Cola creada: 'test'
[broker] Consumidor suscrito a 'test'
[broker] Cliente conectado: ('155.210.154.197', XXXXX)
[broker] Cola ya existe: 'test'
[broker] Entregado msg <uuid> → consumidor en 'test'
...
```

**Consumidor (198)**:
```
[client] Escuchando mensajes...
[Consumidor] Esperando mensajes...
Procesando mensaje: 'Mensaje 0'
Procesando mensaje: 'Mensaje 1'
...
```

**Productor (197)**:
```
Publicado: 'Mensaje 0' -> {'status': 'ok'}
Publicado: 'Mensaje 1' -> {'status': 'ok'}
...
```

---

## Alternativa: editar el host desde línea de comandos

Para no tener que editar los ficheros manualmente en cada máquina,
puedes pasar la IP del broker como variable de entorno si modificas
`cliente.py` para leerla:

```python
import os
host = os.getenv("BROKER_HOST", "localhost")
cliente = Cliente(host=host, port=5555)
```

Y luego lanzar así:

```bash
# En la máquina del productor
BROKER_HOST=155.210.154.196 python3 productor.py

# En la máquina del consumidor
BROKER_HOST=155.210.154.196 python3 consumidor.py
```

---

## Diferencias con la práctica anterior (Java + RMI)

| Aspecto | Java RMI (P. anterior) | Python sockets (P3) |
|---|---|---|
| Compilación | `javac *.java` en cada máquina | No necesaria |
| Registro de objetos | `rmiregistry 32000` en cada máquina | No necesario |
| Parámetro de red | `-Djava.rmi.server.hostname=IP` | `host="IP"` en el cliente |
| Dependencias | JDK instalado | Python 3 instalado |
| Puertos | 32000, 32001, 32002 | Solo 5555 |

---

## Notas importantes

- El broker debe arrancar **antes** que el consumidor y el productor.
- Si el broker cae, los clientes perderán la conexión. Reinicia el broker
  y luego vuelve a lanzar los clientes.
- El puerto 5555 debe estar abierto en el firewall de la máquina 196.
  Si hay problemas de conexión, verificar con:

```bash
# Desde la máquina 197 o 198
nc -zv 155.210.154.196 5555
```

Si responde `Connection to 155.210.154.196 5555 port [tcp] succeeded`
el puerto está accesible.
