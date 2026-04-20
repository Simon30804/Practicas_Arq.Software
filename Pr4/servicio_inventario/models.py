from sqlalchemy import Column, Integer, String, CheckConstraint
from db import Base


class Producto(Base):
    __tablename__ = "productos"
    __table_args__ = (
        CheckConstraint("stock >= 0", name="ck_producto_stock_no_negativo"),
    )

    id = Column(Integer, primary_key=True, index=True)
    nombre = Column(String(120), nullable=False)
    stock = Column(Integer, nullable=False, default=0)