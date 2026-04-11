import json
import socket
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
        
    def siguiente_consumidor(self):
        with self.lock:
            vivo = [cons for cons in self.consumidores if not cons._closed] # Filtramos los consumidores que aún están vivos
            if not vivo:
                return None # Si no hay consumidores vivos, devolvemos None
            consumidor = vivo[self.roundRobinIndex % len(vivo)]
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

    def __new__(cls):
        with cls._lock:
            if cls._instance is None:
                instance = super().__new__(cls)
                instance._queues = {} # Diccionario de colas disponibles en el broker, donde la clave es el nombre de la cola y el valor es una instancia de la clase Queue
                instance._queues_lock = threading.Lock() # Lock para sincronizar el acceso al diccionario de colas
                instance._start_expiration_thread()
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
            print(f"[broker] Cola '{nombre_cola}' no existe — mensaje descartado")
            return {"status": "error", "reason": "queue not found"}

        msg = Message(mensaje)
        consumer = q.siguiente_consumidor()

        if consumer:
            # Entrega inmediata al consumidor disponible
            self._send_to_consumer(consumer, q, msg)
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
            print(f"[broker] Cola '{nombre_cola}' no existe — suscripción fallida")
            return {"status": "error", "reason": "queue not found"}

        with q.lock:
            q.consumidores.append(consumidor)
            
        print(f"[broker] Consumidor suscrito a '{nombre_cola}'")

        # Enviar mensajes pendientes al nuevo consumidor
        while True:
            consumer = q.siguiente_consumidor()
            if consumer is None:
                break
            msg = q.obtener()
            if msg is None:
                break
            self._send_to_consumer(consumidor, q, msg)

        return {"status": "ok"}
    
    # Funciones auxiliares
    def _send_to_consumer(self, consumidor, queue: Queue, mensaje: Message):
        """Envía un mensaje a un consumidor concreto."""
        import json
        payload = json.dumps({
            "status": "message",
            "queue":  queue.name,
            "msg_id": mensaje.id,
            "body":   mensaje.body,
        }) + "\n"
        try:
            consumidor.sendall(payload.encode())
            print(f"[broker] Entregado msg {mensaje.id} → consumidor en '{queue.name}'")
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
                        self._send(response)
        except Exception as e:
            print(f"[server] Error con {self.addr}: {e}")
        finally:
            self._closed = True
            self.conn.close()
            print(f"[server] Desconectado: {self.addr}")

    def _dispatch(self, raw: str) -> dict:
        """Decodifica el JSON y llama al servicio correcto del broker."""
        try:
            req = json.loads(raw)
        except json.JSONDecodeError:
            return {"status": "error", "reason": "invalid JSON"}

        op = req.get("op")

        if op == "declare_queue":
            return self.broker.declarar_cola(req["queue"])

        elif op == "publish":
            return self.broker.publicar(req["queue"], req["body"])

        elif op == "consume":
            # El consumidor se queda suscrito — no devolvemos respuesta aquí
            # inmediatamente; el broker empujará mensajes cuando lleguen.
            result = self.broker.consumir(req["queue"], self.conn)
            return result

        elif op == "list_queues":
            return self.broker.listar_colas()

        elif op == "delete_queue":
            return self.broker.eliminar_cola(req["queue"])

        elif op == "ack":
            # Paso 5 — por ahora respondemos ok
            return {"status": "ok"}

        else:
            return {"status": "error", "reason": f"unknown op: {op}"}

    def sendall(self, data: dict):
        """Envía datos al cliente. Si ocurre un error, marca el consumidor como cerrado."""
        try:
            self.conn.sendall((json.dumps(data) + "\n").encode())
        except Exception as e:
            print(f"[broker] Error enviando datos al cliente {self.addr}: {e}")
            self._closed = True


# Servidor principal
class BrokerServer:
    def __init__(self, host="localhost", port=5555):
        self.host = host
        self.port = port

    def start(self):
        server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server_sock.bind((self.host, self.port))
        server_sock.listen(50)
        print(f"[server] Broker escuchando en {self.host}:{self.port}")

        try:
            while True:
                conn, addr = server_sock.accept()
                handler = ClientHandler(conn, addr)
                handler.start()
        except KeyboardInterrupt:
            print("\n[server] Deteniendo broker...")
        finally:
            server_sock.close()


# Punto de entrada del programa
if __name__ == "__main__":
    server = BrokerServer()
    server.start()
