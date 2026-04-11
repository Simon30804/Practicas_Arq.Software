import socket, json, threading

class Cliente:
    """ Cliente para interactuar con el broker. Proporciona métodos para declarar colas, publicar mensajes y otras operaciones administrativas.
    Lo usan tanto los productores como los consumidores para comunicarse con el broker."""
    def __init__(self, host: str = "localhost", port: int = 5555):
        self.host     = host
        self.port     = port
        self._sock    = None
        self._lock    = threading.Lock()   # Para evitar escrituras simultáneas
        self._buffer  = ""
        self._connect()
    
    def _connect(self):
        self._sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._sock.connect((self.host, self.port))
        print(f"[Cliente] Conectado a al borker {self.host}:{self.port}")

    # Enviar un mensaje al broker y esperar una respuesta, asegurando que solo un hilo pueda enviar a la vez para evitar mezclas de mensajes
    def _enviar(self, mensaje: dict) -> dict:
        """Envía un mensaje (JSON) al broker y espera una respuesta."""
        with self._lock:
            self._sock.sendall((json.dumps(mensaje) + "\n").encode()) # Enviamos el mensaje al broker
            return self._recibir() # Esperamos la respuesta del broker
    
    # Recibir un mensaje del broker, asegurándonos de leer líneas completas para decodificar correctamente el JSON
    def _recibir(self) -> dict:
        """Lee una línea completa del socket y la decodifica como JSON."""
        while "\n" not in self._buffer: # Leemos del socket hasta encontrar un salto de línea, lo que indica el final del mensaje
            dato = self._sock.recv(4096).decode() # Recibimos datos del socket
            if not dato: # Si no recibimos datos, significa que la conexión se ha cerrado
                raise ConnectionError("Conexión cerrada por el broker")
            self._buffer += dato # Agregamos los datos recibidos al buffer
        
        linea, self._buffer = self._buffer.split("\n", 1) # Separamos el mensaje completo del buffer
        return json.loads(linea.strip()) # Devolvemos el mensaje como un diccionario decodificado desde JSON
    
    # Servicios del cliente para interactuar con el broker
    def declarar_cola(self, cola: str) -> dict:
        """ Declara una nueva cola en el broker. Devuelve la respuesta del broker."""
        return self._enviar({"accion": "declarar_cola", "nombre": cola})
    
    def publicar(self, cola: str, mensaje: str) -> dict:
        """ Publica un mensaje en una cola específica. Devuelve la respuesta del broker."""
        return self._enviar({"accion": "publicar", "cola": cola, "mensaje": mensaje})
    
    def consumir(self, cola: str, callback):
        """
        Se suscribe a la cola y arranca un hilo que invoca `callback(body)`
        por cada mensaje recibido. No bloquea el hilo principal.
        """
        resp = self._enviar({"op": "consume", "queue": cola}) # Enviamos una solicitud al broker para consumir mensajes de la cola especificada
        if resp.get("status") != "ok":
            raise RuntimeError(f"Error al suscribirse: {resp}")

        # Hilo dedicado a escuchar mensajes entrantes
        t = threading.Thread(
            target=self._listen_loop,
            args=(callback,),
            daemon=True
        )
        t.start()
        return t

    # Bucle que se ejecuta en un hilo separado para escuchar mensajes del broker y llamar al callback correspondiente
    def _listen_loop(self, callback):
        """Bucle que espera mensajes del broker y llama al callback."""
        print("[client] Escuchando mensajes...")
        while True:
            try:
                msg = self._recibir()
                if msg.get("status") == "message":
                    callback(msg["body"])
            except Exception as e:
                print(f"[client] Conexión cerrada: {e}")
                break

    # Funciones adicionales para listar colas y eliminar colas, útiles para pruebas y administración

    def listar_colas(self) -> list:
        resp = self._enviar({"op": "listar_colas"}) # Enviamos una solicitud al broker para listar las colas disponibles
        return resp.get("queues", [])

    def eliminar_cola(self, cola: str) -> dict:
        return self._enviar({"op": "borrar_cola", "queue": cola}) # Enviamos una solicitud al broker para eliminar una cola específica

    def close(self): 
        self._sock.close() # Cerramos la conexión con el broker
        print("[Cliente] Conexión cerrada")