from fastapi import FastAPI, HTTPException, Depends
from sqlalchemy.orm import Session
from sqlalchemy.exc import SQLAlchemyError, IntegrityError

from db import Base, engine, SessionLocal, get_db
from models import Producto

app = FastAPI(title="Servicio de Inventario")


@app.on_event("startup")
def startup():
    Base.metadata.create_all(bind=engine)

    db = SessionLocal()
    try:
        if db.query(Producto).count() == 0:
            db.add_all([
                Producto(id=1, nombre="Camiseta", stock=10),
                Producto(id=2, nombre="Pantalon", stock=5),
                Producto(id=3, nombre="Zapatillas", stock=0),
            ])
            db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


@app.get("/inventario/{producto_id}")
def consultar_stock(producto_id: int, db: Session = Depends(get_db)):
    producto = db.query(Producto).filter(Producto.id == producto_id).first()
    if not producto:
        raise HTTPException(status_code=404, detail="Producto no encontrado")

    return {
        "producto_id": producto.id,
        "nombre": producto.nombre,
        "stock": producto.stock,
        "disponible": producto.stock > 0,
    }


@app.put("/inventario/{producto_id}/decrementar")
def decrementar_stock(producto_id: int, db: Session = Depends(get_db)):
    try:
        producto = (
            db.query(Producto)
            .filter(Producto.id == producto_id)
            .with_for_update()
            .first()
        )

        if not producto:
            raise HTTPException(status_code=404, detail="Producto no encontrado")
        if producto.stock <= 0:
            raise HTTPException(status_code=409, detail="Sin stock disponible")

        producto.stock -= 1
        db.commit()
        db.refresh(producto)

        return {
            "producto_id": producto.id,
            "nombre": producto.nombre,
            "stock": producto.stock,
        }

    except HTTPException:
        raise
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=409, detail="Conflicto de integridad en stock")
    except SQLAlchemyError:
        db.rollback()
        raise HTTPException(status_code=500, detail="Error de base de datos")


@app.get("/health")
def health():
    return {"status": "ok"}