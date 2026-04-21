import json
import os
import socket
import sys
import uuid, time, threading
from collections import deque

class Message:
    TTL = 300 # El tiempo de vida, son 5 minutos en segundos

    def __init__(self, body: str):
        self.id = str(uuid.uuid4()) # Identificador único del mensaje
        self.body = body
        self.timestamp = time.time() # Marca de tiempo de creación del mensaje, momento de su llegada al broker

    def expirado(self) -> bool:
        return time.time() - self.timestamp > self.TTL # Compara el tiempo actual con el tiempo de creación del mensaje para determinar si ha expirado
    
class Queue:
    def __init__(self, name: str):
        self.name = name
        self.messages = deque() # Cola de mensajes pendientes de ser entregados a los consumidores
        self.consumidores = [] # Conjunto de consumidores suscritos a esta cola
        self.roundRobinIndex = 0 # Índice para implementar la política de round-robin entre los consumidores
        self.lock = threading.Lock() # Lock para sincronizar el acceso a la cola de mensajes y a la lista de consumidores
        self.unacked = {} # Diccionario para rastrear los mensajes entregados pero aún no confirmados por los consumidores (ack pendientes)
        self.unacked_lock = threading.Lock() # Lock para sincronizar el acceso al diccionario de ack pendientes

    def marcar_unacked(self, mensaje: Message, consumidor):
        """Marca un mensaje como entregado pero pendiente de ack por parte del consumidor."""
        with self.unacked_lock:
            self.unacked[mensaje.id] = (mensaje, consumidor)

    def confirmar_ack(self, mensaje_id: str) -> bool:
        """Confirma la recepción de un mensaje por parte del consumidor. Devuelve True si el ack es válido."""
        with self.unacked_lock:
            if mensaje_id in self.unacked:
                del self.unacked[mensaje_id] # Eliminamos el mensaje del diccionario de ack pendientes
                return True
        return False
    
    def reencolar_unacked(self, consumidor):
        """Al desconectarse un consumidor, reencolamos sus mensajes sin confirmar."""
        with self.unacked_lock:
            a_reencolar = [msg for msg, c in self.unacked.values() if c is consumidor]
            self.unacked = {k: v for k, v in self.unacked.items() if v[1] is not consumidor}
        with self.lock:
            for msg in a_reencolar:
                self.messages.appendleft(msg)  # van al frente, son prioritarios
                print(f"[broker] Reencolado msg {msg.id} tras desconexión")

    def publicar(self, mensaje: Message):
        with self.lock:
            self.messages.append(mensaje) # Agregamos un nuevo mensaje a la cola de mensajes
    
    def obtener(self) -> Message:
        with self.lock:
            while self.messages:
                mensaje = self.messages.popleft() # Obtenemos el siguiente mensaje de la cola
                if not mensaje.expirado(): # Verificamos si el mensaje no ha expirado
                    return mensaje # Si el mensaje es válido, lo devolvemos para ser entregado al consumidor
            return None # Si no hay mensajes válidos, devuelve None
        
    def siguiente_consumidor(self, fair: bool = True):
        """
        Devuelve el siguiente consumidor disponible.
        Con fair=True (por defecto), omite los consumidores con mensajes pendientes de ACK.
        """
        with self.lock:
            vivo = [cons for cons in self.consumidores if not cons._closed] # Filtramos los consumidores que aún están vivos
            if not vivo:
                return None # Si no hay consumidores vivos, devolvemos None
            
            if fair:
                with self.unacked_lock:
                    ocupados = {c for _, c in self.unacked.values()} # Obtenemos el conjunto de consumidores que tienen mensajes pendientes de ack
                candidatos = [c for c in vivo if c not in ocupados] # Filtramos los consumidores vivos que no están ocupados con mensajes pendientes de ack
                if not candidatos: # Si no hay consumidores disponibles sin mensajes pendientes, permitimos que cualquier consumidor vivo pueda recibir el mensaje para evitar bloqueos
                    return None # todos están ocupados, devolvemos None para que el mensaje se quede en la cola y se intente entregar más tarde
            else:
                candidatos = vivo # Si no se requiere fairness, todos los consumidores vivos son candidatos

            consumidor = candidatos[self.roundRobinIndex % len(candidatos)] # Seleccionamos el siguiente consumidor en la lista de candidatos utilizando la política de round-robin
            self.roundRobinIndex += 1
            return consumidor # Devolvemos el siguiente consumidor en la política de round-robin
    
    def eliminar_consumidor(self, consumidor):
        with self.lock:
            self.consumidores.remove(consumidor) # Eliminamos un consumidor de la lista de consumidores suscritos a esta cola  

    def eliminar_expirado(self):
        with self.lock:
            self.messages = deque([msg for msg in self.messages if not msg.expirado()]) # Eliminamos los mensajes que han expirado de la cola de mensajes


# Singleton que representa el núcleo del broker, expone los 3 servicios principales: declarar_cola, publicar y consumir.
# Acemás cuenta con un hilo que periódicamente elimina los mensajes expirados de todas las colas, y funciones avanzadas para listar y eliminar colas.
class BrokerCore:
    _instance = None
    _lock = threading.Lock()

    FICHERO_ESTADO = "estado_broker.json"

    def guardar_estado(self):
        """Serializa las colas y mensajes pendientes a disco."""
        estado = {}
        with self._queues_lock:
            for nombre, q in self._queues.items():
                with q.lock:
                    estado[nombre] = [
                        {"id": m.id, "body": m.body, "timestamp": m.timestamp}
                        for m in q.messages if not m.expirado()
                    ]
        with open(self.FICHERO_ESTADO, "w") as f:
            json.dump(estado, f, indent=2)
        print(f"[broker] Estado guardado ({len(estado)} colas)")

    def cargar_estado(self):
        """Recupera colas y mensajes desde disco al arrancar."""
        if not os.path.exists(self.FICHERO_ESTADO):
            return
        with open(self.FICHERO_ESTADO) as f:
            estado = json.load(f)
        with self._queues_lock:
            for nombre, mensajes in estado.items():
                q = Queue(nombre)
                for m_data in mensajes:
                    msg = Message.__new__(Message)
                    msg.id        = m_data["id"]
                    msg.body      = m_data["body"]
                    msg.timestamp = m_data["timestamp"]
                    if not msg.expirado():
                        q.messages.append(msg)
                self._queues[nombre] = q
                print(f"[broker] Cola '{nombre}' restaurada ({len(q.messages)} msgs)")

    def __new__(cls):
        with cls._lock:
            if cls._instance is None:
                instance = super().__new__(cls)
                instance._queues = {} # Diccionario de colas disponibles en el broker, donde la clave es el nombre de la cola y el valor es una instancia de la clase Queue
                instance._queues_lock = threading.Lock() # Lock para sincronizar el acceso al diccionario de colas
                instance._start_expiration_thread()
                instance.cargar_estado() # Cargamos el estado del broker desde disco al arrancar
                cls._instance = instance
            return cls._instance
    
    # Servicio 1: Declarar una cola (idempotente)
    def declarar_cola(self, nombre: str) -> dict:
        """
        Crea la cola si no existe (idempotente).
        Invocado por productores y consumidores.
        """
        with self._queues_lock:
            if nombre not in self._queues:
                self._queues[nombre] = Queue(nombre)
                print(f"[broker] Cola creada: '{nombre}'")
            else:
                print(f"[broker] Cola ya existe: '{nombre}'")
        return {"status": "ok"}
    
    # Servicio 2: Publicar un mensaje en una cola
    def publicar(self, nombre_cola: str, mensaje: str) -> dict:
        """
        Deposita un mensaje en la cola.
        Si la cola no existe, descarta el mensaje.
        Si hay consumidores suscritos, se lo entrega al siguiente en round robin.
        """
        with self._queues_lock:
            q = self._queues.get(nombre_cola)

        if q is None:
            print(f"[broker] Cola '{nombre_cola}' no existe -- mensaje descartado")
            return {"status": "error", "reason": "queue not found"}

        msg = Message(mensaje)
        consumer = q.siguiente_consumidor()

        if consumer:
            # Entrega inmediata al consumidor disponible
            self._enviar_a_consumidor(consumer, q, msg)
        else:
            # Sin consumidores: guardar en cola (máx. 5 min, el TTL que definimos en la clase Message)
            q.publicar(msg)
            print(f"[broker] Mensaje encolado en '{nombre_cola}': {msg.id}")

        return {"status": "ok"}
    
    # Servicio 3: Suscribirse a una cola
    def consumir(self, nombre_cola: str, consumidor) -> dict:
        """
        Agrega un consumidor a la lista de consumidores de la cola.
        Si la cola no existe, devuelve error.
        """
        with self._queues_lock:
            q = self._queues.get(nombre_cola)

        if q is None:
            print(f"[broker] Cola '{nombre_cola}' no existe -- suscripción fallida")
            return {"status": "error", "reason": "queue not found"}

        with q.lock:
            q.consumidores.append(consumidor)
            
        print(f"[broker] Consumidor suscrito a '{nombre_cola}'")

        # Enviar mensajes pendientes al nuevo consumidor
        def _drain(q, consumidor):
            time.sleep(0.1)  # pequeña espera para que el cliente arranque el listener
            while True: 
                msg = q.obtener()
                if msg is None:
                    break #Si no hay mensajes pendientes, salimos del bucle
                # Esperamos a que haya un consumidor disponible para entregar el mensaje
                while True:
                    consumer = q.siguiente_consumidor()
                    if consumer is not None:
                        break
                    time.sleep(0.5) # Todos ocupados, esperamos a que haya un consumidor disponible
                self._enviar_a_consumidor(consumidor, q, msg)

        threading.Thread(target=_drain, args=(q, consumidor), daemon=True).start()

        return {"status": "ok"}
    
    # Funciones auxiliares
    def _enviar_a_consumidor(self, consumidor, queue: Queue, mensaje: Message):
        """Envía un mensaje a un consumidor concreto y lo marca como pendiente de ACK."""
        payload = {
            "status": "message",
            "queue":  queue.name,
            "mensaje_id": mensaje.id,
            "body":   mensaje.body,
        }
        try:
            consumidor._enviar(payload) 
            queue.marcar_unacked(mensaje, consumidor)
            print(f"[broker] Entregado msg {mensaje.id} --> consumidor en '{queue.name}'")
        except Exception as e:
            print(f"[broker] Error enviando mensaje: {e}")
            queue.eliminar_consumidor(consumidor)

    def _start_expiration_thread(self):
        """Inicia un hilo que periódicamente elimina mensajes expirados de todas las colas."""
        def expiration_loop():
            while True:
                time.sleep(60)
                with self._queues_lock:
                    queues = list(self._queues.values())
                for q in queues:
                    q.eliminar_expirado()
                self.guardar_estado() # Guardamos el estado del broker después de eliminar los mensajes expirados
        t = threading.Thread(target=expiration_loop, daemon=True)
        t.start()

     # Funciones avanzadas
    def listar_colas(self) -> dict:
        """Devuelve la lista de colas disponibles en el broker."""
        with self._queues_lock:
            names = list(self._queues.keys())
        return {"status": "ok", "queues": names}

    def eliminar_cola(self, name: str) -> dict:
        """Elimina una cola y todos sus mensajes. Si la cola no existe, devuelve error."""
        with self._queues_lock:
            if name in self._queues:
                del self._queues[name]
                return {"status": "ok"}
        return {"status": "error", "reason": "queue not found"}
    
    def ack(self, nombre_cola: str, mensaje_id: str) -> dict:
        """El consumidor confirma que procesó correctamente un mensaje."""
        with self._queues_lock:
            q = self._queues.get(nombre_cola)
        if q and q.confirmar_ack(mensaje_id):
            print(f"[broker] ACK recibido para msg {mensaje_id} en '{nombre_cola}'")
            return {"status": "ok"}
        return {"status": "error", "reason": "mensaje_id no encontrado"}


# Clase que representa un hilo para manejar la conexión con un cliente (productor o consumidor). Cada vez que el broker acepta una nueva conexión, se crea una instancia de esta clase para manejar la comunicación con ese cliente.
class ClientHandler(threading.Thread):
    def __init__(self, conn, addr):
        super().__init__(daemon=True)
        self.conn = conn
        self.addr = addr
        self._closed = False
        self.broker = BrokerCore() # Referencia al núcleo del broker para invocar los servicios

    def run(self):
        print(f"[broker] Cliente conectado: {self.addr}")
        buffer=""
        try:
            while True:
                data = self.conn.recv(4096).decode()
                if not data:
                    break                    # cliente desconectado
                buffer += data
                # Procesar todas las líneas completas del buffer
                while "\n" in buffer:
                    line, buffer = buffer.split("\n", 1)
                    line = line.strip()
                    if line:
                        response = self._dispatch(line)
                        self._enviar(response)
        except Exception as e:
            print(f"[server] Error con {self.addr}: {e}")
        finally:
            self._closed = True
            self.conn.close()
            #  Reencolar mensajes sin confirmar de este cliente
            with self.broker._queues_lock:
                queues = list(self.broker._queues.values())
            for q in queues:
                q.reencolar_unacked(self)
            print(f"[server] Desconectado: {self.addr}")

    def _dispatch(self, raw: str) -> dict:
        """Decodifica el JSON y llama al servicio correcto del broker."""
        try:
            req = json.loads(raw)
        except json.JSONDecodeError:
            return {"status": "error", "reason": "invalid JSON"}

        accion = req.get("accion")

        if accion == "declarar_cola":
            return self.broker.declarar_cola(req["queue"])

        elif accion == "publicar":
            return self.broker.publicar(req["queue"], req["body"])

        elif accion == "consumir":
            # El consumidor se queda suscrito — no devolvemos respuesta aquí
            # inmediatamente; el broker empujará mensajes cuando lleguen.
            result = self.broker.consumir(req["queue"], self)
            return result

        elif accion == "listar_colas":
            return self.broker.listar_colas()

        elif accion == "eliminar_cola":
            return self.broker.eliminar_cola(req["queue"])

        elif accion == "ack":
            # El consumidor confirma que procesó correctamente un mensaje, el broker lo marca como confirmado y lo elimina de la lista de pendientes de ack
            return self.broker.ack(req["queue"], req["mensaje_id"])

        else:
            return {"status": "error", "reason": f"unknown accion: {accion}"}

    def _enviar(self, data: dict):
        """Envía datos al cliente. Si ocurre un error, marca el consumidor como cerrado."""
        try:
            self.conn.sendall((json.dumps(data) + "\n").encode())
        except Exception as e:
            print(f"[broker] Error enviando datos al cliente {self.addr}: {e}")
            self._closed = True


# Servidor principal
class BrokerServer:
    def __init__(self, host="0.0.0.0", port=5555):
        self.host = host
        self.port = port

    def start(self):
        BrokerCore() # Inicializamos el núcleo del broker (carga estado, inicia hilo de expiración, etc.)

        server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server_sock.bind((self.host, self.port))
        server_sock.listen(50)
        
        # 1. Añadimos un timeout al socket (1 segundo)
        server_sock.settimeout(1.0) 
        
        print(f"[server] Broker escuchando en {self.host}:{self.port}")

        try:
            while True:
                try:
                    conn, addr = server_sock.accept()
                    handler = ClientHandler(conn, addr)
                    handler.start()
                # 2. Capturamos el timeout para que el bucle siga girando y pueda detectar el Ctrl+C
                except socket.timeout:
                    continue 
                except Exception as e:
                    print(f"[server] Error aceptando conexión: {e}")  # ← veremos el error real
                    continue  # no cerramos el servidor, seguimos
                    
        except KeyboardInterrupt:
            print("\n[server] Deteniendo broker (Ctrl+C detectado)...")
            BrokerCore().guardar_estado() # Guardamos el estado del broker antes de cerrar para no perder las colas y mensajes pendientes
        finally:
            server_sock.close()
            # 3. Forzamos la salida del programa para matar cualquier hilo rezagado
            sys.exit(0)


# Punto de entrada del programa
if __name__ == "__main__":
    server = BrokerServer()
    server.start()
