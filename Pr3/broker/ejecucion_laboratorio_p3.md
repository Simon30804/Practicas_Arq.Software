# Ejecución en máquinas del laboratorio — Práctica 3

---

## Distribución de máquinas

| Máquina | IP | Rol |
|---|---|---|
| lab102-196 | 155.210.154.196 | Broker de mensajes |
| lab102-197 | 155.210.154.197 | Productor |
| lab102-198 | 155.210.154.195 | Consumidor |

---

## Paso 1 — Encender las máquinas remotas

Nos conectamos a la máquina central de la universidad:

```bash
ssh a869800@central.cps.unizar.es
```

Encendemos las tres máquinas:

```bash
/usr/local/etc/wake -y lab102-196
/usr/local/etc/wake -y lab102-197
/usr/local/etc/wake -y lab102-195
```

Verificamos que responden:

```bash
ping 155.210.154.196
ping 155.210.154.197
ping 155.210.154.195
```

---

## Paso 2 — Copiar los ficheros a cada máquina

Desde nuestra máquina local, copiamos los ficheros de la práctica:

```bash
# Crear directorio en cada máquina remota
ssh a869800@155.210.154.196 "mkdir -p ~/practica3"
ssh a869800@155.210.154.197 "mkdir -p ~/practica3"
ssh a869800@155.210.154.195 "mkdir -p ~/practica3"

# Copiar broker a la máquina 196
scp broker_server.py cliente.py a869800@155.210.154.196:~/practica3/

# Copiar productor a la máquina 197
scp cliente.py productor.py a869800@155.210.154.197:~/practica3/

# Copiar consumidor a la máquina 195
scp cliente.py consumidor.py a869800@155.210.154.195:~/practica3/
```

---

## Paso 3 — Arrancar el broker (máquina 196)

```bash
ssh a869800@155.210.154.196
cd ~/practica3
python3 broker_server.py
```

Debemos ver:
```
[server] Broker escuchando en 0.0.0.0:5555
```

Mantenemos esta sesión SSH abierta.

---

## Paso 4 — Arrancar el consumidor (máquina 195)

Abrimos otra terminal y nos conectamos a la máquina 195.

Lanza el consumidor:

```bash
BROKER_HOST=155.210.154.196 python3 consumidor.py
```
---

## Paso 5 — Arrancar el productor (máquina 197)

Abrimos otra terminal y nos conectamos a la máquina 197.

Lanzamos el productor:

```bash
BROKER_HOST=155.210.154.196 python3 productor.py
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

**Consumidor (195)**:
```
[client] Escuchando mensajes...
[Consumidor] Esperando mensajes...
Procesando mensaje: 'Mensaje 0'
[Consumidor] ACK enviado para d92d5385-336e-421b-8144-8f4576abaa51
Procesando mensaje: 'Mensaje 2'
[Consumidor] ACK enviado para 387db302-9c63-4b01-9d10-aac91efb111f
Procesando mensaje: 'Mensaje 4'
[Consumidor] ACK enviado para c2b6a1fc-dc30-4c5d-9b11-93dde6a617ed
Procesando mensaje: 'Mensaje 6'
[Consumidor] ACK enviado para fa7deba5-0d64-4ee4-8d37-16ec850c25ce
Procesando mensaje: 'Mensaje 8'
[Consumidor] ACK enviado para 14dc5f8d-d22c-4ed0-9fc1-893182fa6da9
...
```

Solo procesa los de numero par en la primera ronda, la siguiente se quedan guardados en la cola como pendientes, podemos verlo en estado_broker.json. Al desconectar el consumidor y conectarlo de nuevo comienza a consumir de manera automatica los mensajes pendientes.
Esto se debe a que simulamos que el consumidor 'trabaja' con un sleep(2), mientras que el productor envía mensajes con un sleep(1), esto hace que el consumidor no pueda consumir 1 de cada 2, pues está procesando otro mensaje.

**Productor (197)**:
```
Publicado: 'Mensaje 0' -> {'status': 'ok'}
Publicado: 'Mensaje 1' -> {'status': 'ok'}
...
```

---


Para no tener que editar los ficheros manualmente en cada máquina,
pasamos la IP del broker como variable de entorno, para leerla:

En consumidor.py y en productor.py
```python
import os
host = os.getenv("BROKER_HOST", "localhost")
cliente = Cliente(host=host, port=5555)
```

Y luego lanzamos así:

```bash
# En la máquina del productor
BROKER_HOST=155.210.154.196 python3 productor.py

# En la máquina del consumidor
BROKER_HOST=155.210.154.196 python3 consumidor.py
```

## Notas importantes

- El broker debe arrancar **antes** que el consumidor y el productor.
- Si el broker cae, los clientes perderán la conexión. Reinicia el broker
  y luego vuelve a lanzar los clientes.

