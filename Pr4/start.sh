#!/bin/bash

# ── Colores ───────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
PR3_BROKER="$BASE_DIR/../Pr3/broker"

echo -e "${YELLOW}[arranque] Iniciando infraestructura...${NC}"

# ── 1. Docker Compose ─────────────────────────────────────────
echo -e "${YELLOW}[arranque] Levantando bases de datos...${NC}"
docker compose -f "$BASE_DIR/docker-compose.yml" up -d

# Esperar a que las BBDDs estén healthy
echo -e "${YELLOW}[arranque] Esperando a que las BBDDs estén listas...${NC}"
until docker inspect postgres-inventario --format='{{.State.Health.Status}}' 2>/dev/null | grep -q "healthy"; do
    sleep 2
done
echo -e "${GREEN}[arranque] PostgreSQL listo${NC}"

until docker inspect mariadb-pedidos --format='{{.State.Health.Status}}' 2>/dev/null | grep -q "healthy"; do
    sleep 2
done
echo -e "${GREEN}[arranque] MariaDB listo${NC}"

# ── 2. Broker ─────────────────────────────────────────────────
echo -e "${YELLOW}[arranque] Arrancando broker...${NC}"
cd "$PR3_BROKER"
python broker_server.py &
BROKER_PID=$!
sleep 2
echo -e "${GREEN}[arranque] Broker PID: $BROKER_PID${NC}"

# ── 3. Servicio de Inventario ─────────────────────────────────
echo -e "${YELLOW}[arranque] Arrancando Servicio de Inventario...${NC}"
cd "$BASE_DIR/servicio_inventario"
python -m uvicorn main:app --host 0.0.0.0 --port 8002 &
INVENTARIO_PID=$!
sleep 3
echo -e "${GREEN}[arranque] Inventario PID: $INVENTARIO_PID${NC}"

# ── 4. Servicio de Pedidos ────────────────────────────────────
echo -e "${YELLOW}[arranque] Arrancando Servicio de Pedidos...${NC}"
cd "$BASE_DIR/servicio_pedidos"
python -m uvicorn main:app --host 0.0.0.0 --port 8001 &
PEDIDOS_PID=$!
sleep 3
echo -e "${GREEN}[arranque] Pedidos PID: $PEDIDOS_PID${NC}"

# ── 5. Servicio de Notificaciones ─────────────────────────────
echo -e "${YELLOW}[arranque] Arrancando Servicio de Notificaciones...${NC}"
cd "$BASE_DIR/servicio_notificaciones"
python main.py &
NOTIFICACIONES_PID=$!
sleep 2
echo -e "${GREEN}[arranque] Notificaciones PID: $NOTIFICACIONES_PID${NC}"

# ── 6. API Gateway ────────────────────────────────────────────
echo -e "${YELLOW}[arranque] Arrancando API Gateway...${NC}"
cd "$BASE_DIR/api_gateway"
python -m uvicorn main:app --host 0.0.0.0 --port 8000 &
GATEWAY_PID=$!
sleep 2
echo -e "${GREEN}[arranque] API Gateway PID: $GATEWAY_PID${NC}"

# ── Resumen ───────────────────────────────────────────────────
echo ""
echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}  Todos los servicios arrancados     ${NC}"
echo -e "${GREEN}=====================================${NC}"
echo -e "  API Gateway:    http://localhost:8000"
echo -e "  Pedidos:        http://localhost:8001"
echo -e "  Inventario:     http://localhost:8002"
echo -e "  Broker:         localhost:5555"
echo ""
echo -e "${YELLOW}PIDs activos:${NC}"
echo -e "  Broker:         $BROKER_PID"
echo -e "  Inventario:     $INVENTARIO_PID"
echo -e "  Pedidos:        $PEDIDOS_PID"
echo -e "  Notificaciones: $NOTIFICACIONES_PID"
echo -e "  Gateway:        $GATEWAY_PID"
echo ""
echo -e "${RED}Para parar todo: bash stop.sh${NC}"

# Guardar PIDs para el script de parada
echo "$BROKER_PID $INVENTARIO_PID $PEDIDOS_PID $NOTIFICACIONES_PID $GATEWAY_PID" > "$BASE_DIR/.pids"

# Mantener el script vivo
wait