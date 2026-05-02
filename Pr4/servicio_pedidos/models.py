from sqlalchemy import Column, Integer, String, DateTime
from datetime import datetime
from db import Base, engine

class Pedido(Base):
    __tablename__ = "pedidos"
    id          = Column(Integer, primary_key=True, index=True)
    producto_id = Column(Integer, nullable=False)
    estado      = Column(String(50), default="completado")
    creado_en   = Column(DateTime, default=datetime.utcnow)

Base.metadata.create_all(bind=engine)