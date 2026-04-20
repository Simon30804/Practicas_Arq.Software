import os
from dotenv import load_dotenv
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

load_dotenv(
    os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env"),
    override=False
)

def _build_connect_args() -> dict:
    return {
        "host": os.getenv("INVENTARIO_DB_HOST", "localhost"),
        "port": int(os.getenv("INVENTARIO_DB_PORT", "5433")),
        "dbname": os.getenv("INVENTARIO_DB_NAME", "inventario"),
        "user": os.getenv("INVENTARIO_DB_USER", "user"),
        "password": os.getenv("INVENTARIO_DB_PASSWORD", "pass"),
    }


engine = create_engine(
    "postgresql+psycopg://",
    connect_args=_build_connect_args(),
    pool_pre_ping=True,
    pool_recycle=1800,
)

SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)
Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()