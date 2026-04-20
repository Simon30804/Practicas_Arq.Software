import os
from dotenv import load_dotenv
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

load_dotenv(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env"),
    override=False
)

engine = create_engine(
    "mysql+pymysql://",
    connect_args={
        "host":     os.getenv("PEDIDOS_DB_HOST",     "localhost"),
        "port":     int(os.getenv("PEDIDOS_DB_PORT", "3307")),
        "database": os.getenv("PEDIDOS_DB_NAME",     "pedidos"),  
        "user":     os.getenv("PEDIDOS_DB_USER",     "user"),
        "password": os.getenv("PEDIDOS_DB_PASSWORD", "pass"),
    },
    pool_pre_ping=True,
    pool_recycle=1800,
)

SessionLocal = sessionmaker(bind=engine)
Base         = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()