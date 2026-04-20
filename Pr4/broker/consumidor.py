import time
from cliente import Cliente

cliente = None # Variable global para almacenar la instancia del cliente y poder usarla dentro del callback `procesarMensaje`

def procesarMensaje(mensaje: str, mensaje_id: str, cola: str):
    print(f"Procesando mensaje: '{mensaje}'")
    time.sleep(2)                          # simulamos trabajo
    cliente.ack(cola, mensaje_id)          # confirmamos al broker
    print(f"[Consumidor] ACK enviado para {mensaje_id}")

def main():
    global cliente
    cliente = Cliente() # Creamos una instancia del cliente para conectarnos al broker 

    cliente.declarar_cola("test") # Declaramos la cola "test" para asegurarnos de que existe antes de intentar consumir mensajes

    hilo = cliente.consumir("test", procesarMensaje) # Servicio 3: Nos suscribimos a la cola "test" y proporcionamos el callback `procesarMensaje` para procesar cada mensaje recibido

    print("[Consumidor] Esperando mensajes...") # Indicamos que el consumidor está listo para recibir mensajes
    try:
        while hilo.is_alive(): # Mantenemos el programa en ejecución mientras el hilo de consumo esté activo
            time.sleep(1) # Esperamos 1 segundo antes de verificar nuevamente si el hilo sigue activo
    except KeyboardInterrupt:
        print("[Consumidor] Deteniendo consumidor...") # Indicamos que se está deteniendo el consumidor al recibir una interrupción por teclado
        cliente.close() # Detenemos el hilo de consumo al recibir una interrupción por teclado

if __name__ == "__main__":
    main() # Ejecutamos la función principal para iniciar el proceso de consumo de mensajes del broker