import os
import sys
import time
from dotenv import load_dotenv
from cliente import Cliente as BrokerCliente

load_dotenv(override=False)

broker = None

def procesar_notificacion(mensaje: str, mensaje_id: str, cola: str):
    """Callback invocado por el broker al recibir un evento."""
    print(f"[Notificaciones] Notificacion enviada: {mensaje}")
    print(f"[Notificaciones] ID mensaje: {mensaje_id}")
    try:
        # Confirmar al broker que el mensaje fue procesado
        broker.ack(cola, mensaje_id)
        print(f"[Notificaciones] ACK enviado: {mensaje_id}")
    except Exception as e:
        print(f"[Notificaciones] Error enviando ACK: {e}")

def main():
    global broker
    broker = BrokerCliente(
        host=os.getenv("BROKER_HOST", "localhost"),
        port=int(os.getenv("BROKER_PORT", "5555"))
    )
    broker.declarar_cola("notificaciones")
    hilo = broker.consumir("notificaciones", procesar_notificacion)
    print("[Notificaciones] Esperando eventos...")
    try:
        while hilo.is_alive():
            time.sleep(1)
    except KeyboardInterrupt:
        print("[Notificaciones] Deteniendo...")
        broker.close()

if __name__ == "__main__":
    main()