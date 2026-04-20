from dotenv import load_dotenv
load_dotenv(override=False)

import os
import sys
import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from db import SessionLocal
from models import Pedido
from cliente import Cliente as BrokerCliente

broker = BrokerCliente(
    host=os.getenv("BROKER_HOST", "localhost"),
    port=int(os.getenv("BROKER_PORT", "5555"))
)
broker.declarar_cola("notificaciones")

# ── URLs de otros servicios ───────────────────────────────────
INVENTARIO_URL = os.getenv("INVENTARIO_URL", "http://localhost:8002")

# ── App ───────────────────────────────────────────────────────
app = FastAPI(title="Servicio de Pedidos")

class PedidoRequest(BaseModel):
    producto_id: int

@app.post("/pedido")
def crear_pedido(req: PedidoRequest):
    """
    Flujo completo de una compra:
    1. Consulta stock en el Servicio de Inventario
    2. Si hay stock, guarda el pedido en MariaDB
    3. Decrementa el stock
    4. Publica evento en el broker para notificaciones
    """
    # 1. Consultar stock
    try:
        resp = httpx.get(f"{INVENTARIO_URL}/inventario/{req.producto_id}", timeout=5)
    except Exception:
        raise HTTPException(status_code=503, detail="Servicio de inventario no disponible")

    if resp.status_code == 404:
        raise HTTPException(status_code=404, detail="Producto no encontrado")

    datos = resp.json()
    if not datos["disponible"]:
        raise HTTPException(status_code=409, detail="Sin stock disponible")

    # 2. Guardar pedido en BD
    db = SessionLocal()
    pedido = Pedido(producto_id=req.producto_id, estado="completado")
    db.add(pedido)
    db.commit()
    db.refresh(pedido)
    pedido_id = pedido.id
    db.close()

    # 3. Decrementar stock
    httpx.put(f"{INVENTARIO_URL}/inventario/{req.producto_id}/decrementar", timeout=5)

    # 4. Publicar evento en el broker
    broker.publicar(
        "notificaciones",
        f"Pedido {pedido_id} completado: producto {datos['nombre']}"
    )

    return {
        "pedido_id":   pedido_id,
        "producto_id": req.producto_id,
        "producto":    datos["nombre"],
        "estado":      "completado"
    }

@app.get("/health")
def health():
    return {"status": "ok"}