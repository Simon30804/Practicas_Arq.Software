# Comandos de arranque — Práctica 4

Ejecutar en este orden cada vez que retomes el desarrollo.

---

## 1 — Verificar que Docker está corriendo

Abre Docker Desktop y espera a que el icono de la ballena esté activo.

---

## 2 — Arrancar Minikube

```powershell
minikube start --driver=docker
```

Verificar que está activo:

```powershell
minikube status
kubectl get nodes
```

---

## 3 — Arrancar PostgreSQL (Servicio de Inventario)

```powershell
docker start postgres-test
```

Si el contenedor no existe todavía (primera vez):

```powershell
docker run -d `
  --name postgres-test `
  -e POSTGRES_USER=user `
  -e POSTGRES_PASSWORD=pass `
  -e POSTGRES_DB=inventario `
  -p 5433:5432 `
  postgres:14
```

Verificar que está corriendo:

```powershell
docker ps | findstr postgres
```

---

## 4 — Arrancar el broker de mensajes (de la práctica 3)

```powershell
cd broker
python broker_server.py
```

---

## 5 — Arrancar el Servicio de Inventario

En una terminal nueva:

```powershell
cd servicio_inventario
python -m uvicorn main:app --host 0.0.0.0 --port 8002 --reload
```

Verificar: http://localhost:8002/health

---

## 6 — Arrancar el Servicio de Pedidos (cuando esté implementado)

```powershell
cd servicio_pedidos
python -m uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

Verificar: http://localhost:8001/health

---

## 7 — Arrancar el API Gateway (cuando esté implementado)

```powershell
cd api_gateway
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Verificar: http://localhost:8000/health

---

## 8 — Arrancar el Servicio de Notificaciones (cuando esté implementado)

```powershell
cd servicio_notificaciones
python main.py
```

---

## Resumen de puertos

| Servicio               | Puerto |
|------------------------|--------|
| API Gateway            | 8000   |
| Servicio de Pedidos    | 8001   |
| Servicio de Inventario | 8002   |
| Broker de mensajes     | 5555   |
| PostgreSQL             | 5433   |
| MariaDB                | 3306   |

---

## Parar todo al terminar

```powershell
# Parar Minikube
minikube stop

# Parar contenedores Docker
docker stop postgres-test
```
