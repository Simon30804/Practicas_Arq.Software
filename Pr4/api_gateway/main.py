import os
import httpx
from pathlib import Path
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Depends
from fastapi.security import APIKeyHeader
from pydantic import BaseModel

load_dotenv(Path(__file__).resolve().parent.parent / ".env")

# Configuración 
API_TOKEN     = os.getenv("API_TOKEN", "mi_clave_secreta")
PEDIDOS_URL   = os.getenv("PEDIDOS_URL", "http://localhost:8001")
INVENTARIO_URL = os.getenv("INVENTARIO_URL", "http://localhost:8002")

# Seguridad 
api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)

def validar_token(api_key: str = Depends(api_key_header)):
    """Valida la cabecera X-API-Key. Devuelve 401 si no coincide."""
    if api_key != API_TOKEN:
        raise HTTPException(status_code=401, detail="No autorizado")
    return api_key

# App 
app = FastAPI(title="API Gateway")

class CompraRequest(BaseModel):
    producto_id: int

@app.post("/compra")
def realizar_compra(
    req: CompraRequest,
    _: str = Depends(validar_token)
):
    """
    Punto de entrada principal.
    Valida el token y reenvía la petición al Servicio de Pedidos.
    """
    try:
        resp = httpx.post(
            f"{PEDIDOS_URL}/pedido",
            json={"producto_id": req.producto_id},
            timeout=10
        )
    except Exception:
        raise HTTPException(status_code=503, detail="Servicio de pedidos no disponible")

    return resp.json()


@app.get("/inventario/{producto_id}")
def obtener_inventario(producto_id: int, _: str = Depends(validar_token)):
    """
    Consulta el stock de un producto a través del Servicio de Inventario.
    """
    try:
        resp = httpx.get(f"{INVENTARIO_URL}/inventario/{producto_id}", timeout=10)
    except Exception:
        raise HTTPException(status_code=503, detail="Servicio de inventario no disponible")

    return resp.json()

@app.get("/health")
def health():
    return {"status": "ok"}