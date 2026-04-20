#!/bin/bash

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

echo -e "${YELLOW}[parada] Deteniendo servicios...${NC}"

# Matar procesos Python por PID
if [ -f "$BASE_DIR/.pids" ]; then
    PIDS=$(cat "$BASE_DIR/.pids")
    for PID in $PIDS; do
        if kill -0 "$PID" 2>/dev/null; then
            kill "$PID"
            echo -e "${GREEN}[parada] Proceso $PID detenido${NC}"
        fi
    done
    rm "$BASE_DIR/.pids"
else
    # Fallback: matar todos los procesos uvicorn y broker
    pkill -f "uvicorn main:app" 2>/dev/null
    pkill -f "broker_server.py" 2>/dev/null
    echo -e "${GREEN}[parada] Procesos Python detenidos${NC}"
fi

# Parar Docker Compose
echo -e "${YELLOW}[parada] Tumbando bases de datos...${NC}"
docker compose -f "$BASE_DIR/docker-compose.yml" down
echo -e "${GREEN}[parada] Bases de datos detenidas${NC}"

echo ""
echo -e "${GREEN}Todo detenido correctamente.${NC}"