# Guía de arranque y depuración del sistema
## Arquitectura de microservicios — Práctica 3 + 4

---

## Índice

1. Requisitos previos
2. Instalación desde cero (máquina nueva)
3. Arranque del sistema en local
4. Arranque del sistema en Kubernetes
5. Comandos de depuración
6. Solución de problemas frecuentes

---

## 1. Requisitos previos

### Software necesario

| Herramienta | Versión mínima | Instalación |
|---|---|---|
| Python | 3.9+ | https://www.python.org/downloads/ |
| Docker Desktop | Última | https://www.docker.com/products/docker-desktop/ |
| Minikube | Última | `winget install Kubernetes.minikube` |
| kubectl | Última | `winget install Kubernetes.kubectl` |
| Git | Última | https://git-scm.com/downloads |

### Verificar instalaciones

```bash
python --version
docker --version
minikube version
kubectl version --client
git --version
```

---

## 2. Instalación desde cero (máquina nueva)

### 2.1 Clonar el repositorio

```bash
git clone <URL_DEL_REPO>
cd <NOMBRE_DEL_REPO>
```

### 2.2 Instalar dependencias Python

```bash
# Dependencias del Servicio de Inventario
cd servicio_inventario
pip install fastapi uvicorn sqlalchemy "psycopg[binary]" python-dotenv

# Dependencias del Servicio de Pedidos
cd ../servicio_pedidos
pip install fastapi uvicorn sqlalchemy pymysql httpx python-dotenv

# Dependencias del API Gateway
cd ../api_gateway
pip install fastapi uvicorn httpx python-dotenv

# Dependencias del Servicio de Notificaciones
cd ../servicio_notificaciones
pip install python-dotenv

cd ..
```

### 2.3 Configurar el .env global

El fichero `.env` se encuentra en la raíz del proyecto (`Pr4/`).
Ajusta las IPs y puertos si es necesario, pero los valores por defecto
funcionan directamente en una máquina nueva:

```dotenv
# PostgreSQL (Servicio de Inventario)
DB_HOST=127.0.0.1
DB_PORT=5433
DB_NAME=inventario
DB_USER=user
DB_PASSWORD=pass

# MariaDB (Servicio de Pedidos)
MARIADB_HOST=127.0.0.1
MARIADB_PORT=3307
MARIADB_DB=pedidos
MARIADB_USER=user
MARIADB_PASSWORD=pass

# Broker de mensajes
BROKER_HOST=localhost
BROKER_PORT=5555

# URLs internas entre servicios
INVENTARIO_URL=http://localhost:8002
PEDIDOS_URL=http://localhost:8001

# API Gateway
API_TOKEN=mi_clave_secreta
```

### 2.4 Arrancar Minikube (primera vez)

```bash
minikube start --driver=docker
```

---

## 3. Arranque del sistema en LOCAL (sin Kubernetes)

Usar este modo para desarrollo y pruebas rápidas.
Abre una terminal separada para cada paso.

### Terminal 1 — Bases de datos (Docker Compose)

```bash
cd Pr4
docker compose up -d

# Verificar que están healthy
docker compose ps
```

Espera a que ambos contenedores muestren `healthy` antes de continuar.

### Terminal 2 — Broker de mensajes

```bash
cd Pr3/broker
python broker_server.py
```

Debes ver: `[server] Broker escuchando en 0.0.0.0:5555`

### Terminal 3 — Servicio de Inventario

```bash
cd Pr4/servicio_inventario
python -m uvicorn main:app --host 0.0.0.0 --port 8002
```

Debes ver: `Application startup complete`

### Terminal 4 — Servicio de Pedidos

```bash
cd Pr4/servicio_pedidos
python -m uvicorn main:app --host 0.0.0.0 --port 8001
```

Debes ver: `[Cliente] Conectado al broker localhost:5555`

### Terminal 5 — Servicio de Notificaciones

```bash
cd Pr4/servicio_notificaciones
python main.py
```

Debes ver: `[Notificaciones] Esperando eventos...`

### Terminal 6 — API Gateway

```bash
cd Pr4/api_gateway
python -m uvicorn main:app --host 0.0.0.0 --port 8000
```

### Probar el sistema en local

```bash
# Sin token — debe devolver 401
curl -X POST http://localhost:8000/compra \
  -H "Content-Type: application/json" \
  -d '{"producto_id": 1}'

# Con token — debe devolver pedido completado
curl -X POST http://localhost:8000/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 1}'

# Producto sin stock (producto_id: 3) — debe devolver 409
curl -X POST http://localhost:8000/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 3}'
```

### Parar el sistema local

```bash
# Parar bases de datos
cd Pr4
docker compose down

# El resto de procesos se paran con Ctrl+C en cada terminal
```

---

## 4. Arranque del sistema en KUBERNETES (Minikube)

### 4.1 Arrancar Minikube

```bash
minikube start --driver=docker
minikube status   # verificar que está Running
```

### 4.2 Construir y cargar imágenes

Ejecutar desde la raíz de `Pr4/`:

```bash
# Construir imágenes
docker build -t broker:v1 ../Pr3/broker/
docker build -t servicio-inventario:v1 servicio_inventario/
docker build -t servicio-pedidos:v1 servicio_pedidos/
docker build -t servicio-notificaciones:v1 servicio_notificaciones/
docker build -t api-gateway:v1 api_gateway/

# Cargar en Minikube
minikube image load broker:v1
minikube image load servicio-inventario:v1
minikube image load servicio-pedidos:v1
minikube image load servicio-notificaciones:v1
minikube image load api-gateway:v1

# Verificar que están cargadas
minikube image ls | grep -E "broker|servicio|api-gateway"
```

### 4.3 Activar el addon de Ingress

Solo necesario la primera vez:

```bash
minikube addons enable ingress

# Esperar a que el controlador esté Running (puede tardar 3-5 min)
kubectl get pods -n ingress-nginx
```

### 4.4 Desplegar todos los recursos

```bash
kubectl apply -f k8s/

# Verificar que todo se creó
kubectl get all
```

### 4.5 Verificar que todos los pods están Running

```bash
kubectl get pods -w
```

Espera hasta ver todos en estado `1/1 Running`:

```
api-gateway-xxx                1/1     Running
broker-xxx                     1/1     Running
mariadb-xxx                    1/1     Running
postgres-xxx                   1/1     Running
servicio-inventario-xxx        1/1     Running
servicio-notificaciones-xxx    1/1     Running
servicio-pedidos-xxx           1/1     Running
```

### 4.6 Exponer el Ingress (en terminal separada)

```bash
minikube tunnel
```

Dejar corriendo. Requiere permisos de administrador en Windows.

### 4.7 Configurar el dominio local

Abrir PowerShell como administrador y ejecutar una sola vez:

```powershell
Add-Content C:\Windows\System32\drivers\etc\hosts "127.0.0.1 tienda.local"
```

### 4.8 Probar el sistema en Kubernetes

```bash
# Sin token — debe devolver 401
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -d '{"producto_id": 1}'

# Con token — debe devolver pedido completado
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 1}'

# Producto sin stock — debe devolver 409
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 3}'
```

### 4.9 Parar el sistema en Kubernetes

```bash
# Eliminar todos los recursos
kubectl delete -f k8s/

# Parar Minikube
minikube stop
```

---

## 5. Comandos de depuración

### Estado general del clúster

```bash
# Ver todos los pods y su estado
kubectl get pods

# Ver todos los recursos (pods, services, deployments)
kubectl get all

# Ver pods en tiempo real
kubectl get pods -w

# Ver el Ingress y su IP
kubectl get ingress

# Ver los Services
kubectl get svc

# Ver los Secrets
kubectl get secrets

# Ver los PVCs
kubectl get pvc
```

### Logs de cada servicio

```bash
# Logs del broker
kubectl logs deployment/broker

# Logs del API Gateway
kubectl logs deployment/api-gateway

# Logs del Servicio de Pedidos
kubectl logs deployment/servicio-pedidos

# Logs del Servicio de Inventario
kubectl logs deployment/servicio-inventario

# Logs del Servicio de Notificaciones
kubectl logs deployment/servicio-notificaciones

# Logs en tiempo real (follow)
kubectl logs deployment/broker -f

# Últimas N líneas
kubectl logs deployment/broker --tail=50

# Logs desde hace X segundos
kubectl logs deployment/broker --since=60s

# Logs de un pod específico (no deployment)
kubectl logs <nombre-del-pod>

# Logs del pod anterior (si el actual crasheó)
kubectl logs deployment/servicio-pedidos --previous
```

### Inspeccionar recursos

```bash
# Descripción detallada de un pod (eventos, variables de entorno, etc.)
kubectl describe pod <nombre-del-pod>

# Descripción de un deployment
kubectl describe deployment servicio-pedidos

# Ver variables de entorno inyectadas en un deployment
kubectl describe deployment api-gateway | grep -A5 Environment

# Ver el contenido de un Secret (en base64)
kubectl get secret secret-api -o yaml

# Decodificar un valor de un Secret
kubectl get secret secret-api -o jsonpath='{.data.API_TOKEN}' | base64 -d
```

### Ejecutar comandos dentro de un pod

```bash
# Abrir una shell dentro de un pod
kubectl exec -it deployment/broker -- /bin/sh

# Ejecutar un comando puntual
kubectl exec -it deployment/broker -- python -c "print('ok')"

# Verificar conectividad entre pods
kubectl exec -it deployment/servicio-pedidos -- curl http://servicio-inventario:8002/health
kubectl exec -it deployment/servicio-pedidos -- curl http://broker:5555
```

### Reiniciar servicios

```bash
# Reiniciar un deployment (útil tras actualizar imagen)
kubectl rollout restart deployment broker
kubectl rollout restart deployment servicio-pedidos
kubectl rollout restart deployment servicio-inventario
kubectl rollout restart deployment servicio-notificaciones
kubectl rollout restart deployment api-gateway

# Reiniciar todos a la vez
kubectl rollout restart deployment --all
```

### Actualizar una imagen

```bash
# 1. Reconstruir sin caché
docker build --no-cache -t servicio-pedidos:v1 servicio_pedidos/

# 2. Eliminar la imagen vieja de Minikube
minikube ssh "docker rmi -f servicio-pedidos:v1"

# 3. Cargar la nueva
minikube image load servicio-pedidos:v1

# 4. Reiniciar el deployment
kubectl rollout restart deployment servicio-pedidos

# 5. Verificar
kubectl get pods -w
```

### Verificar bases de datos

```bash
# Conectar a PostgreSQL
kubectl exec -it deployment/postgres -- psql -U user -d inventario

# Consultas útiles en PostgreSQL
# \dt                    -- listar tablas
# SELECT * FROM productos;
# \q                     -- salir

# Conectar a MariaDB
kubectl exec -it deployment/mariadb -- mariadb -u user -ppass pedidos

# Consultas útiles en MariaDB
# SHOW TABLES;
# SELECT * FROM pedidos;
# EXIT;
```

### Minikube

```bash
# Ver estado
minikube status

# Ver imágenes cargadas
minikube image ls

# Abrir dashboard web
minikube dashboard

# Ver IP del clúster
minikube ip

# Acceder por SSH al nodo
minikube ssh
```

---

## 6. Solución de problemas frecuentes

### Pod en estado ErrImageNeverPull
La imagen no está cargada en Minikube.
```bash
minikube image load <nombre-imagen>:v1
kubectl rollout restart deployment <nombre>
```

### Pod en estado CrashLoopBackOff
El contenedor arranca y falla. Ver logs:
```bash
kubectl logs deployment/<nombre> --previous
```

### No puedo conectar a tienda.local
Verificar que `minikube tunnel` está corriendo en otra terminal.
Verificar que el hosts file tiene la entrada:
```bash
cat /etc/hosts | grep tienda
# Windows: cat C:\Windows\System32\drivers\etc\hosts | grep tienda
```

### Servicio devuelve "servicio no disponible"
Verificar que todos los pods están Running y que los Services existen:
```bash
kubectl get pods
kubectl get svc
```

### El broker no recibe conexiones
Verificar que el Service del broker existe y apunta al puerto correcto:
```bash
kubectl get svc broker
kubectl describe svc broker
```

### Las variables de entorno no se leen correctamente
Los .env locales no deben copiarse dentro de las imágenes Docker.
Verificar que existe .dockerignore en cada servicio:
```bash
cat servicio_pedidos/.dockerignore   # debe contener: .env
```

### Error de base64 inválido en un Secret
Regenerar el valor correcto:
```bash
echo -n "mi_valor" | base64
```
Actualizar el yaml y reaplicar:
```bash
kubectl delete secret <nombre>
kubectl apply -f k8s/<nombre>.yaml
```

---

## Resumen de puertos

| Componente | Puerto local | Puerto en K8s |
|---|---|---|
| API Gateway | 8000 | ClusterIP (Ingress en 80) |
| Servicio de Pedidos | 8001 | ClusterIP 8001 |
| Servicio de Inventario | 8002 | ClusterIP 8002 |
| Broker de mensajes | 5555 | ClusterIP 5555 |
| PostgreSQL | 5433 | ClusterIP 5432 |
| MariaDB | 3307 | ClusterIP 3306 |
