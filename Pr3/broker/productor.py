import time
import os
from cliente import Cliente

def main():
    cliente = Cliente( # Creamos una instancia del cliente para conectarnos al broker 
        host=os.getenv("BROKER_HOST", "127.0.0.1"),
        port=int(os.getenv("BROKER_PORT", "5555"))
    ) 

    # Declaramos una cola llamada "test"
    cliente.declarar_cola("test") # Servicio 1

    for i in range(10):
        mensaje = f"Mensaje {i}" # Creamos un mensaje con un número de secuencia
        respuesta = cliente.publicar("test", mensaje) # Publicamos el mensaje en la cola "test", Servicio 2
        print(f"Publicado: '{mensaje}' -> {respuesta}") # Imprimimos el mensaje publicado
        time.sleep(1) # Esperamos 1 segundo antes de publicar el siguiente mensaje

    # Intentamos publicar un mensaje en una cola no declarada para probar el manejo de errores
    respuesta_error = cliente.publicar("cola_no_declarada", "Este mensaje se perderá") # Intentamos publicar en una cola que no existe
    print(f"Intento de publicación en cola no declarada -> {respuesta_error}") # Imprimimos la respuesta del broker para el intento de publicación en una cola no declarada

    cliente.close() # Cerramos la conexión con el broker al finalizar

if __name__ == "__main__":
    main() # Ejecutamos la función principal para iniciar el proceso de publicación de mensajes en el broker