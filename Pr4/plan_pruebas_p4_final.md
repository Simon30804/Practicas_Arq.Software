# Plan de pruebas — Práctica 4: Arquitectura de microservicios

---

## Índice

1. Entorno de pruebas
2. Pruebas de infraestructura
3. Pruebas del API Gateway (seguridad y SecDevOps)
4. Pruebas del Servicio de Inventario
5. Pruebas del Servicio de Pedidos
6. Pruebas del Servicio de Notificaciones
7. Pruebas de flujo completo end-to-end
8. Pruebas de Kubernetes
9. Resultados esperados resumidos

---

## 1. Entorno de pruebas

Todas las pruebas se ejecutan sobre **Kubernetes (Minikube)**, que es el entorno
de entrega final. El dominio `tienda.local` está mapeado a `127.0.0.1` en el
fichero hosts del sistema.

### Arranque del sistema

```bash
# 1. Arrancar Minikube
minikube start --driver=docker

# 2. Desplegar todos los recursos
kubectl apply -f k8s/

# 3. Esperar a que todos los pods estén Running
kubectl get pods -w

# 4. Arrancar el tunnel (terminal separada, mantener abierta)
minikube tunnel
```

### Verificación previa

```bash
# Confirmar que tienda.local resuelve correctamente
ping tienda.local
# Debe responder desde 127.0.0.1

# Confirmar que el Ingress tiene IP asignada
kubectl get ingress
# La columna ADDRESS debe mostrar una IP
```

### URL base de todas las pruebas

```
http://tienda.local
```

---

## 2. Pruebas de infraestructura

---

### PT-01 — Todos los pods en estado Running

**Objetivo**: verificar que todos los componentes del sistema arrancan correctamente en Kubernetes.

**Pasos**:
```bash
kubectl get pods
```

**Resultado esperado**:
```
NAME                                     READY   STATUS    RESTARTS
api-gateway-xxx                          1/1     Running   0
broker-xxx                               1/1     Running   0
mariadb-xxx                              1/1     Running   0
postgres-xxx                             1/1     Running   0
servicio-inventario-xxx                  1/1     Running   0
servicio-notificaciones-xxx              1/1     Running   0
servicio-pedidos-xxx                     1/1     Running   0
```

**Criterio de éxito**: los 7 pods muestran `1/1 Running`. Ninguno en estado `Error`, `CrashLoopBackOff` o `Pending`.

---

### PT-02 — Health check del sistema

**Objetivo**: verificar que el API Gateway (único punto de entrada) responde correctamente a través del Ingress.

**Pasos**:
```bash
curl http://tienda.local/health
```

**Resultado esperado**:
- Código HTTP: `200`
```json
{"status": "ok"}
```

**Criterio de éxito**: el Ingress enruta correctamente la petición al API Gateway.

---

### PT-03 — Conectividad de bases de datos

**Objetivo**: verificar que PostgreSQL y MariaDB están accesibles desde dentro del clúster.

**Pasos**:
```bash
# PostgreSQL
kubectl exec -it deployment/postgres -- psql -U user -d inventario -c "SELECT 1"

# MariaDB
kubectl exec -it deployment/mariadb -- mariadb -u user -ppass -e "SELECT 1"
```

**Resultado esperado**: ambas devuelven `1` sin errores.

**Criterio de éxito**: las bases de datos están operativas con los datos de acceso correctos.

---

### PT-04 — Conectividad del broker desde los servicios

**Objetivo**: verificar que el broker de mensajes es accesible por DNS interno desde el Servicio de Pedidos.

**Pasos**:
```bash
kubectl exec -it deployment/servicio-pedidos -- python -c "
import socket
s = socket.socket()
s.connect(('broker', 5555))
print('Broker accesible')
s.close()
"
```

**Resultado esperado**:
```
Broker accesible
```

**Criterio de éxito**: la resolución DNS interna de Kubernetes resuelve `broker` correctamente y el puerto 5555 está accesible.

---

### PT-05 — PVCs en estado Bound

**Objetivo**: verificar que los volúmenes persistentes de las bases de datos están correctamente asignados.

**Pasos**:
```bash
kubectl get pvc
```

**Resultado esperado**:
```
NAME          STATUS   VOLUME   CAPACITY
pvc-mariadb   Bound    ...      1Gi
pvc-postgres  Bound    ...      1Gi
```

**Criterio de éxito**: ambos PVCs en estado `Bound`. Sin PVCs en estado `Pending`.

---

### PT-06 — Secrets correctamente configurados

**Objetivo**: verificar que los Secrets de Kubernetes existen y contienen las claves necesarias.

**Pasos**:
```bash
kubectl get secrets
kubectl describe secret secret-api
kubectl describe secret secret-mariadb
kubectl describe secret secret-postgres
```

**Resultado esperado**:
- `secret-api` contiene la clave `API_TOKEN`
- `secret-mariadb` contiene `MARIADB_PASSWORD` y `MARIADB_ROOT_PASSWORD`
- `secret-postgres` contiene `POSTGRES_PASSWORD`

**Criterio de éxito**: los tres Secrets existen con sus claves correctas. Ningún pod falla por Secret no encontrado.

---

## 3. Pruebas del API Gateway (seguridad y SecDevOps)

El API Gateway es el **único punto de entrada** al sistema desde el exterior,
gestionado a través del Ingress. Implementa validación de token siguiendo
las directrices SecDevOps del guión.

---

### PT-07 — Petición sin token → 401

**Objetivo**: verificar que el API Gateway rechaza peticiones que no incluyen la cabecera `X-API-Key`.

**Pasos**:
```bash
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -d '{"producto_id": 1}'
```

**Resultado esperado**:
- Código HTTP: `401`
```json
{"detail": "No autorizado"}
```

**Criterio de éxito**: la petición es rechazada en el API Gateway sin llegar al Servicio de Pedidos.

---

### PT-08 — Token incorrecto → 401

**Objetivo**: verificar que cualquier token distinto al configurado es rechazado.

**Pasos**:
```bash
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: token_incorrecto" \
  -d '{"producto_id": 1}'
```

**Resultado esperado**:
- Código HTTP: `401`
```json
{"detail": "No autorizado"}
```

**Criterio de éxito**: el sistema no procesa la petición con credenciales incorrectas.

---

### PT-09 — Token correcto → 200

**Objetivo**: verificar que una petición con el token válido es aceptada y procesada.

**Pasos**:
```bash
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 1}'
```

**Resultado esperado**:
- Código HTTP: `200`
```json
{
  "pedido_id": 1,
  "producto_id": 1,
  "producto": "Camiseta",
  "estado": "completado"
}
```

**Criterio de éxito**: la compra se procesa correctamente de extremo a extremo.

---

### PT-10 — Token leído del Secret de Kubernetes (SecDevOps)

**Objetivo**: demostrar que el token **nunca está hardcodeado** en el código fuente — se lee dinámicamente del Secret de Kubernetes, siguiendo las directrices SecDevOps del guión.

**Pasos**:

1. Verificar el token actual almacenado en el Secret:
```bash
kubectl get secret secret-api -o jsonpath='{.data.API_TOKEN}' | base64 -d
# Debe mostrar: mi_clave_secreta
```

2. Cambiar el valor del Secret sin tocar el código:
```bash
echo -n "nuevo_token" | base64
# Resultado: bnVldm9fdG9rZW4=

kubectl patch secret secret-api -p '{"data":{"API_TOKEN":"bnVldm9fdG9rZW4="}}'
kubectl rollout restart deployment api-gateway
kubectl rollout status deployment api-gateway
```

3. Probar que el nuevo token funciona:
```bash
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: nuevo_token" \
  -d '{"producto_id": 1}'
# Debe devolver 200
```

4. Probar que el token antiguo ya no funciona:
```bash
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 1}'
# Debe devolver 401
```

5. Restaurar el token original:
```bash
kubectl patch secret secret-api -p '{"data":{"API_TOKEN":"bWlfY2xhdmVfc2VjcmV0YQ=="}}'
kubectl rollout restart deployment api-gateway
```

**Criterio de éxito**: el API Gateway acepta el nuevo token y rechaza el antiguo, sin haber modificado ninguna línea de código. Demuestra la correcta implementación SecDevOps.

---

## 4. Pruebas del Servicio de Inventario

El Servicio de Inventario es interno al clúster. Se accede desde el exterior
únicamente a través del API Gateway (`/inventario/{id}`).

---

### PT-11 — Consultar stock de productos existentes

**Objetivo**: verificar que el servicio devuelve el stock correcto de los productos del seed inicial.

**Pasos**:
```bash
curl http://tienda.local/inventario/1 -H "X-API-Key: mi_clave_secreta"
curl http://tienda.local/inventario/2 -H "X-API-Key: mi_clave_secreta"
curl http://tienda.local/inventario/3 -H "X-API-Key: mi_clave_secreta"
```

**Resultado esperado**:
```json
{"producto_id": 1, "nombre": "Camiseta",   "stock": 10, "disponible": true}
{"producto_id": 2, "nombre": "Pantalon",   "stock": 5,  "disponible": true}
{"producto_id": 3, "nombre": "Zapatillas", "stock": 0,  "disponible": false}
```

**Criterio de éxito**: los datos coinciden con los insertados en el seed inicial y están persistidos en PostgreSQL.

---

### PT-12 — Consultar producto inexistente → 404

**Objetivo**: verificar que el sistema maneja correctamente IDs de producto inexistentes.

**Pasos**:
```bash
curl http://tienda.local/inventario/999 -H "X-API-Key: mi_clave_secreta"
```

**Resultado esperado**:
- Código HTTP: `404`
```json
{"detail": "Producto no encontrado"}
```

**Criterio de éxito**: el sistema devuelve 404 sin errores internos.

---

### PT-13 — Stock se decrementa tras una compra

**Objetivo**: verificar que el Servicio de Inventario actualiza el stock en PostgreSQL cuando el Servicio de Pedidos realiza una compra.

**Pasos**:
```bash
# Consultar stock inicial
curl http://tienda.local/inventario/1 -H "X-API-Key: mi_clave_secreta"

# Realizar una compra
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 1}'

# Consultar stock actualizado
curl http://tienda.local/inventario/1 -H "X-API-Key: mi_clave_secreta"
```

**Resultado esperado**: el stock del producto 1 disminuye en 1 unidad tras la compra.

**Criterio de éxito**: el stock se actualiza correctamente en PostgreSQL. El Servicio de Pedidos llama síncronamente al Servicio de Inventario (comunicación HTTP REST interna).

---

## 5. Pruebas del Servicio de Pedidos

---

### PT-14 — Crear pedido con stock disponible → 200

**Objetivo**: verificar el flujo completo del Servicio de Pedidos: consulta inventario, guarda en MariaDB y publica evento.

**Pasos**:
```bash
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 2}'
```

**Resultado esperado**:
- Código HTTP: `200`
```json
{
  "pedido_id": 1,
  "producto_id": 2,
  "producto": "Pantalon",
  "estado": "completado"
}
```

**Verificar registro en MariaDB**:
```bash
kubectl exec -it deployment/mariadb -- mariadb -u user -ppass pedidos \
  -e "SELECT * FROM pedidos ORDER BY id DESC LIMIT 1;"
```

**Criterio de éxito**: el pedido existe en MariaDB con estado `completado`.

---

### PT-15 — Compra de producto sin stock → 409

**Objetivo**: verificar que no se puede comprar un producto con stock 0.

**Pasos**:
```bash
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 3}'
```

**Resultado esperado**:
- Código HTTP: `409`
```json
{"detail": "Sin stock disponible"}
```

**Criterio de éxito**: el pedido no se crea en MariaDB y el stock no se modifica.

---

### PT-16 — Compra de producto inexistente → 404

**Objetivo**: verificar que no se puede comprar un producto que no existe en el inventario.

**Pasos**:
```bash
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 999}'
```

**Resultado esperado**:
- Código HTTP: `404`
```json
{"detail": "Producto no encontrado"}
```

**Criterio de éxito**: el Servicio de Pedidos recibe el 404 del Servicio de Inventario y lo propaga correctamente.

---

### PT-17 — Evento publicado en el broker tras compra exitosa

**Objetivo**: verificar que el Servicio de Pedidos publica un evento asíncrono en el broker tras completar una compra.

**Pasos**:
```bash
# Realizar una compra
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 1}'

# Verificar en los logs del broker
kubectl logs deployment/broker --tail=10
```

**Resultado esperado en los logs del broker**:
```
[broker] Mensaje encolado en 'notificaciones': <uuid>
```
o si el consumidor está activo:
```
[broker] Entregado msg <uuid> → consumidor en 'notificaciones'
```

**Criterio de éxito**: el evento se publica correctamente en la cola `notificaciones` del broker.

---

## 6. Pruebas del Servicio de Notificaciones

---

### PT-18 — Notificación recibida y mostrada en stdout

**Objetivo**: verificar que el Servicio de Notificaciones consume el evento del broker y lo imprime por stdout como indica el guión.

**Pasos**:
```bash
# Realizar una compra
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 2}'

# Ver los logs del Servicio de Notificaciones
kubectl logs deployment/servicio-notificaciones --tail=10
```

**Resultado esperado**:
```
[Notificaciones] Notificacion enviada: Pedido X completado: producto Pantalon
[Notificaciones] ID mensaje: <uuid>
[Notificaciones] ACK enviado: <uuid>
```

**Criterio de éxito**: el log aparece en stdout del Servicio de Notificaciones con el nombre del producto y el ID del pedido.

---

### PT-19 — Durabilidad: mensajes encolados se entregan al reconectar

**Objetivo**: verificar que los mensajes publicados mientras el Servicio de Notificaciones estaba caído se entregan cuando vuelve a arrancar. Demuestra la durabilidad del broker de la práctica 3.

**Pasos**:

1. Parar el Servicio de Notificaciones:
```bash
kubectl scale deployment/servicio-notificaciones --replicas=0
kubectl get pods -l app=servicio-notificaciones -w
# Esperar hasta que desaparezca
```

2. Publicar 3 pedidos:
```bash
curl -X POST http://tienda.local/compra -H "Content-Type: application/json" -H "X-API-Key: mi_clave_secreta" -d '{"producto_id": 2}'
curl -X POST http://tienda.local/compra -H "Content-Type: application/json" -H "X-API-Key: mi_clave_secreta" -d '{"producto_id": 1}'
curl -X POST http://tienda.local/compra -H "Content-Type: application/json" -H "X-API-Key: mi_clave_secreta" -d '{"producto_id": 2}'
```

3. Verificar que los mensajes están en el broker:
```bash
kubectl exec -it deployment/broker -- sh -c 'cat /app/estado_broker.json'
# Debe mostrar 3 mensajes en la cola "notificaciones"
```

4. Restaurar el Servicio de Notificaciones:
```bash
kubectl scale deployment/servicio-notificaciones --replicas=1
kubectl rollout status deployment/servicio-notificaciones
```

5. Verificar que recibe los 3 mensajes:
```bash
kubectl logs deployment/servicio-notificaciones --tail=20
```

**Resultado esperado**: el servicio recibe y procesa los 3 mensajes encolados. La cola queda vacía:
```bash
kubectl exec -it deployment/broker -- sh -c 'cat /app/estado_broker.json'
# "notificaciones": []
```

**Criterio de éxito**: ningún mensaje se pierde aunque el consumidor estuviera caído. Demuestra la integración correcta del broker de la práctica 3.

---

## 7. Pruebas de flujo completo end-to-end

---

### PT-20 — Flujo completo de compra exitosa (4 verificaciones)

**Objetivo**: verificar el flujo completo de la arquitectura de microservicios de extremo a extremo, tal como lo describe el guión.

**Pasos**:

```bash
# Consultar stock inicial
curl http://tienda.local/inventario/2 -H "X-API-Key: mi_clave_secreta"

# Realizar la compra
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 2}'
```

**Verificación 1 — API Gateway responde 200**:
```json
{"pedido_id": X, "producto_id": 2, "producto": "Pantalon", "estado": "completado"}
```

**Verificación 2 — Stock decrementado en PostgreSQL**:
```bash
curl http://tienda.local/inventario/2 -H "X-API-Key: mi_clave_secreta"
# stock debe ser uno menos que antes
```

**Verificación 3 — Pedido guardado en MariaDB**:
```bash
kubectl exec -it deployment/mariadb -- mariadb -u user -ppass pedidos \
  -e "SELECT * FROM pedidos ORDER BY id DESC LIMIT 1;"
```

**Verificación 4 — Notificación en stdout del Servicio de Notificaciones**:
```bash
kubectl logs deployment/servicio-notificaciones --tail=5
# [Notificaciones] Notificacion enviada: Pedido X completado: producto Pantalon
```

**Criterio de éxito**: las 4 verificaciones son correctas. El flujo completo Cliente → Ingress → API Gateway → Servicio de Pedidos → Servicio de Inventario → Broker → Servicio de Notificaciones funciona de extremo a extremo.


---

### PT-22 — Múltiples compras simultáneas (concurrencia)

**Objetivo**: verificar que el sistema maneja peticiones concurrentes sin corrupción de datos gracias al bloqueo `with_for_update()` del Servicio de Inventario.

**Pasos**:
```bash
# Consultar stock inicial del producto 1
curl http://tienda.local/inventario/1 -H "X-API-Key: mi_clave_secreta"

# Lanzar 5 compras en paralelo
for i in {1..5}; do
  curl -s -X POST http://tienda.local/compra \
    -H "Content-Type: application/json" \
    -H "X-API-Key: mi_clave_secreta" \
    -d '{"producto_id": 1}' &
done
wait

# Verificar stock final
curl http://tienda.local/inventario/1 -H "X-API-Key: mi_clave_secreta"
```

**Resultado esperado**: el stock final bajó exactamente en 5 unidades. No hay pedidos duplicados.

**Verificar en MariaDB**:
```bash
kubectl exec -it deployment/mariadb -- mariadb -u user -ppass pedidos \
  -e "SELECT COUNT(*) FROM pedidos;"
```

**Criterio de éxito**: stock consistente, sin duplicados en la tabla de pedidos.

---

## 8. Pruebas de Kubernetes

---

### PT-23 — Persistencia de datos tras reinicio de MariaDB

**Objetivo**: verificar que los pedidos sobreviven al reinicio del pod de MariaDB gracias al PVC.

**Pasos**:
```bash
# 1. Crear un pedido y anotar su ID
curl -X POST http://tienda.local/compra \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 1}'

# 2. Anotar el pedido_id de la respuesta

# 3. Reiniciar el pod de MariaDB
kubectl rollout restart deployment mariadb
kubectl get pods -w   # esperar a que vuelva a Running

# 4. Verificar que el pedido sigue en la BD
kubectl exec -it deployment/mariadb -- mariadb -u user -ppass pedidos \
  -e "SELECT * FROM pedidos ORDER BY id DESC LIMIT 5;"
```

**Criterio de éxito**: el pedido creado antes del reinicio sigue existiendo. El PVC garantiza la persistencia de los datos.

---

### PT-24 — Persistencia de datos tras reinicio de PostgreSQL

**Objetivo**: verificar que el inventario sobrevive al reinicio del pod de PostgreSQL gracias al PVC.

**Pasos**:
```bash
# 1. Consultar stock actual
curl http://tienda.local/inventario/1 -H "X-API-Key: mi_clave_secreta"

# 2. Reiniciar el pod de PostgreSQL
kubectl rollout restart deployment postgres
kubectl get pods -w   # esperar a que vuelva a Running

# 3. Verificar que el stock sigue siendo el mismo
curl http://tienda.local/inventario/1 -H "X-API-Key: mi_clave_secreta"
```

**Criterio de éxito**: el stock no se resetea tras el reinicio. Los datos del inventario persisten gracias al PVC.

---

### PT-25 — Resolución DNS interna entre servicios

**Objetivo**: verificar que los servicios se comunican por nombre DNS interno de Kubernetes, no por IP directa.

**Pasos**:
```bash
kubectl exec -it deployment/servicio-pedidos -- \
  python -c "import socket; print('mariadb:', socket.gethostbyname('mariadb'))"

kubectl exec -it deployment/servicio-pedidos -- \
  python -c "import socket; print('inventario:', socket.gethostbyname('servicio-inventario'))"

kubectl exec -it deployment/servicio-pedidos -- \
  python -c "import socket; print('broker:', socket.gethostbyname('broker'))"
```

**Resultado esperado**: cada nombre resuelve a una IP interna del clúster (rango `10.x.x.x`):
```
mariadb: 10.x.x.x
inventario: 10.x.x.x
broker: 10.x.x.x
```

**Criterio de éxito**: el DNS interno de Kubernetes funciona correctamente. Los servicios se referencian por nombre, no por IP (que puede cambiar).

---

### PT-26 — Ingress como único punto de entrada

**Objetivo**: verificar que los servicios internos NO son accesibles desde el exterior directamente — solo el API Gateway está expuesto.

**Pasos**:
```bash
# Intentar acceder directamente al Servicio de Pedidos (debe fallar)
curl http://tienda.local/pedido \
  -H "Content-Type: application/json" \
  -H "X-API-Key: mi_clave_secreta" \
  -d '{"producto_id": 1}'

# Intentar acceder directamente a las bases de datos (debe fallar)
curl http://tienda.local:3306
curl http://tienda.local:5432
```

**Resultado esperado**: las rutas internas no expuestas por el Ingress devuelven `404` o no responden.

**Criterio de éxito**: el único punto de entrada externo es el API Gateway a través del Ingress. La arquitectura respeta el principio de mínima exposición del guión.

---

## 9. Resultados esperados resumidos

| ID | Descripción | Componente | Resultado esperado |
|---|---|---|---|
| PT-01 | Todos los pods Running | Kubernetes | 7 pods 1/1 Running |
| PT-02 | Health check del sistema | Ingress + Gateway | 200 |
| PT-03 | Conectividad bases de datos | BBDDs | SELECT 1 OK |
| PT-04 | Conectividad broker | Broker | Conexión TCP OK |
| PT-05 | PVCs en Bound | Kubernetes | Bound 1Gi x2 |
| PT-06 | Secrets configurados | Kubernetes | 3 Secrets con claves |
| PT-07 | Sin token → 401 | API Gateway | 401 No autorizado |
| PT-08 | Token incorrecto → 401 | API Gateway | 401 No autorizado |
| PT-09 | Token correcto → 200 | API Gateway | 200 completado |
| PT-10 | Token leído de Secret K8s | SecDevOps | 200 nuevo / 401 antiguo |
| PT-11 | Consultar stock existente | Inventario | 200 con datos seed |
| PT-12 | Producto inexistente | Inventario | 404 |
| PT-13 | Stock decrementa tras compra | Inventario + Pedidos | Stock -1 en PostgreSQL |
| PT-14 | Compra con stock | Pedidos | 200 + registro MariaDB |
| PT-15 | Compra sin stock | Pedidos | 409 |
| PT-16 | Compra producto inexistente | Pedidos | 404 |
| PT-17 | Evento publicado en broker | Pedidos + Broker | Mensaje en cola |
| PT-18 | Notificación en stdout | Notificaciones | Log en consola |
| PT-19 | Durabilidad del broker | Notificaciones + Broker | Mensajes entregados al reconectar |
| PT-20 | Flujo completo end-to-end | Todos | 4 verificaciones OK |
| PT-21 | Agotamiento de stock | End-to-end | 200x5 + 409 |
| PT-22 | Compras simultáneas | End-to-end | Stock consistente |
| PT-23 | Persistencia MariaDB tras reinicio | Kubernetes + PVC | Datos conservados |
| PT-24 | Persistencia PostgreSQL tras reinicio | Kubernetes + PVC | Datos conservados |
| PT-25 | DNS interno entre servicios | Kubernetes | IPs 10.x.x.x |
| PT-26 | Ingress único punto de entrada | Kubernetes | Servicios internos no accesibles |
