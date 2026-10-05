# Banco XYZ — Suite BFF (Backend for Frontend)

**PBY2203 — Desarrollo Backend III — Semana 8 (Exp3)**
Actividad: *Desarrollando microservicios y resiliencia en la nube con Spring Cloud*

> Continuidad de las semanas anteriores. Esta entrega (Semana 8) agrega:
> 1. **OAuth2.0** con un servidor de autorización propio (`auth-server`, Spring Authorization Server).
> 2. **Dockerización** de todos los módulos (un `Dockerfile` por servicio).
> 3. **`docker-compose.yml`** que orquesta todo el sistema con un solo comando.
>
> Se mantienen la tolerancia a fallos (Resilience4j) y la arquitectura de eventos (Kafka) de la Semana 7
> ([`docs/ADR-02-arquitectura-eventos.md`](docs/ADR-02-arquitectura-eventos.md)). La decisión de OAuth2 está en
> [`docs/ADR-03-oauth2.md`](docs/ADR-03-oauth2.md).

## 1. Objetivo del proyecto

Implementar el patrón **Backend for Frontend (BFF)** para el Banco XYZ: un BFF por canal y un microservicio por
dominio de datos, preparados para un entorno Cloud **seguro** (OAuth2), **resiliente** (Resilience4j) y
**desplegable** (Docker). Datos base: <https://github.com/KariVillagran/bank_legacy_data>.

| Servicio | Puerto | Rol |
|---|---|---|
| `auth-server` | 9000 | Servidor de autorización OAuth2/OIDC: emite los access tokens (JWT, RS256) |
| `bff-web` | 8081 | BFF canal Web — datos completos, paginación y filtros |
| `bff-mobile` | 8082 | BFF canal Móvil — respuestas livianas |
| `bff-atm` | 8083 | BFF canal Cajero — superficie mínima, doble factor |
| `ms-cuentas` | 8090 | Saldo e interés de cada cuenta |
| `ms-movimientos` | 8091 | Historial de retiros/depósitos (productor Kafka) |
| `ms-transacciones` | 8092 | Feed global de transacciones (consumidor Kafka) |
| `config-server` | 8888 | Spring Cloud Config (configuración de `ms-cuentas`) |
| `eureka-server` | 8761 | Service discovery (Eureka) |
| `kafka` | 9092 | Broker de eventos (KRaft, un nodo) |
| `postgres` | 5432 | PostgreSQL (opcional; los ms usan H2 en memoria) |

## 2. Arquitectura de seguridad (OAuth2.0)

```
 Usuario/App ──(authorization_code [+PKCE])──▶ auth-server ──▶ access token (JWT)
      │                                                                │
      └──────────── Authorization: Bearer <token> ──▶ BFF (Resource Server: valida JWT con /oauth2/jwks)
                                                        │
                       BFF ──(client_credentials)──▶ auth-server ──▶ token de servicio (scope ms.read / ms.write)
                                                        │
                                                        └── Bearer ──▶ ms-* (Resource Server)
```

Clientes OAuth2 registrados en `auth-server`:

| client_id | Tipo | Grant | Scopes | Uso |
|---|---|---|---|---|
| `web-client` | confidencial | authorization_code, refresh_token, client_credentials | openid, web.read, web.write | Canal Web (token 60 min) |
| `mobile-client` | público | authorization_code **+ PKCE**, refresh_token | openid, mobile.read | App móvil (token 15 min) |
| `atm-client` | confidencial | client_credentials | atm.device | Identidad del cajero físico (token 3 min) |
| `bff-web-svc`, `bff-mobile-svc`, `bff-atm-svc` | confidencial | client_credentials | ms.read, ms.write | BFF → microservicios |

Usuarios demo (formulario de login del `auth-server`): `admin.web / Admin#2026` (roles `WEB_ADMIN`, `WEB_USER`),
`operador.web / Oper#2026` (`WEB_USER`), `cliente.app / Cliente#2026` (`MOBILE_USER`).
El token incluye el claim `roles`; cada BFF exige su rol (`WEB_USER`, `MOBILE_USER`, `ATM_DEVICE`).

**Cajero (2 factores):** (1) Bearer del cliente `atm-client`; (2) login con tarjeta + PIN
(`POST /api/atm/auth/login`) que entrega un token de sesión ligado a la cuenta, enviado en la cabecera `X-Atm-Session`.
Tarjetas demo: `4551000000000001`/`1234` → cuenta 101; `4551000000000002`/`5678` → cuenta 105.

**Microservicios:** `GET` requiere scope `ms.read`; `POST/PATCH` requieren `ms.write`. Sin token → 401.

> Los secretos de este repositorio son valores de demostración. En producción se entregan por variables de entorno
> (ver `docker-compose.yml`) y no se versionan.

## 3. Estructura del código

```
Exp3_S8_Grupo20/
├── pom.xml                     # POM padre (multi-módulo Maven)
├── docker-compose.yml          # Orquestación de TODO el sistema
├── .dockerignore
├── auth-server/                # Servidor OAuth2/OIDC            (+ Dockerfile)
├── bff-core/                   # Librería compartida: conversor JWT→roles, token de servicio, clientes HTTP, excepciones
├── bff-web/  bff-mobile/  bff-atm/          # BFF por canal (Resource Servers + Resilience4j)   (+ Dockerfile c/u)
├── ms-cuentas/  ms-movimientos/  ms-transacciones/   # Microservicios de dominio (Resource Servers) (+ Dockerfile c/u)
├── config-server/  eureka-server/           # Spring Cloud Config y Eureka                      (+ Dockerfile c/u)
├── docs/                       # ADR-02 (eventos/resiliencia), ADR-03 (OAuth2)
├── data/                       # CSV oficiales (bank_legacy_data)
└── evidencia/                  # Capturas / salidas de consola (ver evidencia/README.md)
```

## 4. Cómo ejecutar

### Opción A — Todo con Docker (recomendado)

Requisitos: Docker y Docker Compose v2.

```bash
docker compose up --build -d     # construye las 9 imágenes y levanta todo
docker compose ps                # todos deben estar "running"
docker compose logs -f auth-server
docker compose down              # detener (añade -v para borrar volúmenes)
```
La primera construcción tarda unos minutos (Maven descarga dependencias). Los servicios se reintentan solos
(`restart: unless-stopped`) si Kafka o el `auth-server` aún no están listos.

### Opción B — Sin Docker para los servicios (solo Kafka en Docker)

Requisitos: Java 17, Maven 3.9+.

```bash
docker compose up -d kafka
mvn clean install
# En terminales separadas, en este orden:
mvn -pl auth-server       spring-boot:run
mvn -pl config-server     spring-boot:run     # ms-cuentas toma su puerto/BD desde aquí
mvn -pl eureka-server     spring-boot:run
mvn -pl ms-cuentas        spring-boot:run
mvn -pl ms-movimientos    spring-boot:run
mvn -pl ms-transacciones  spring-boot:run
mvn -pl bff-web           spring-boot:run
mvn -pl bff-mobile        spring-boot:run
mvn -pl bff-atm           spring-boot:run
```

Cada microservicio carga los CSV oficiales al iniciar (H2 en memoria) e informa en el log cuántos registros cargó
y cuántos rechazó por datos inválidos. Cuentas de ejemplo: `101, 105, 106, 108, 109, 117, 118, 122, 124, 127, 128,
130, 132, 133, 143, 144, 147`.

## 5. Cómo probar la seguridad

1. **Metadatos OIDC:** `GET http://localhost:9000/.well-known/openid-configuration`
2. **Microservicio sin token → 401:** `curl -i http://localhost:8090/cuentas`
3. **Token de servicio y llamada directa a un ms → 200:**
   ```bash
   TOKEN=$(curl -s -u bff-web-svc:bff-web-svc-cambiar -d grant_type=client_credentials -d scope=ms.read \
     http://localhost:9000/oauth2/token | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
   curl -i http://localhost:8090/cuentas -H "Authorization: Bearer $TOKEN"
   ```
4. **Canal Web (usuario) con Postman:** Authorization → OAuth 2.0 → *Authorization Code*;
   Auth URL `http://localhost:9000/oauth2/authorize`, Token URL `http://localhost:9000/oauth2/token`,
   Client ID `web-client`, Secret `web-secret-cambiar`, Scope `openid web.read`,
   Callback `https://oauth.pstmn.io/v1/callback`. Inicia sesión con `admin.web`.
   Luego `GET http://localhost:8081/api/web/cuentas` → 200 (sin token → 401; token de máquina sin rol → 403).
5. **Canal Móvil:** igual con `mobile-client` (sin secreto, activar *Use PKCE*, Scope `openid mobile.read`, usuario `cliente.app`);
   `GET http://localhost:8082/api/mobile/cuentas/101/resumen`.
6. **Canal Cajero:**
   ```bash
   DEV=$(curl -s -u atm-client:atm-secret-cambiar -d grant_type=client_credentials -d scope=atm.device \
     http://localhost:9000/oauth2/token | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
   SES=$(curl -s -X POST http://localhost:8083/api/atm/auth/login -H "Authorization: Bearer $DEV" \
     -H "Content-Type: application/json" -d '{"numeroTarjeta":"4551000000000001","pin":"1234"}' \
     | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")
   curl -i http://localhost:8083/api/atm/cuentas/101/saldo -H "Authorization: Bearer $DEV" -H "X-Atm-Session: $SES"
   curl -i -X POST http://localhost:8083/api/atm/cuentas/101/retiro -H "Authorization: Bearer $DEV" \
     -H "X-Atm-Session: $SES" -H "Content-Type: application/json" -d '{"monto":10000}'
   ```
   (El login del cajero devuelve el token de sesión en el campo `accessToken`.)

### Verificar eventos y resiliencia (Semana 7)
- Tras un retiro, `ms-movimientos` loguea `Evento movimiento-registrado publicado` y `ms-transacciones`
  `Transaccion generada desde evento` (`docker compose logs ms-movimientos ms-transacciones`).
- `GET /api/web/transacciones` (con token web) muestra la nueva transacción.
- Tolerancia a fallos: `docker compose stop ms-cuentas` y pedir un saldo → `503`; estado del circuito en
  `http://localhost:8083/actuator/circuitbreakers` (también en 8081 y 8082).

## 6. Endpoints por BFF

**Web** (`ROLE_WEB_USER`): `GET /api/web/cuentas?tipo=&page=&size=`, `GET /api/web/cuentas/{id}`,
`GET /api/web/cuentas/{id}/historial-anual`, `GET /api/web/transacciones?desde=&hasta=&tipo=&page=&size=`

**Móvil** (`ROLE_MOBILE_USER`): `GET /api/mobile/cuentas/{id}/resumen`, `GET /api/mobile/movimientos/recientes`

**Cajero** (Bearer `atm.device` + `X-Atm-Session`): `POST /api/atm/auth/login`, `GET /api/atm/cuentas/{id}/saldo`,
`POST /api/atm/cuentas/{id}/retiro`

**Microservicios** (scope `ms.read` / `ms.write`): `ms-cuentas` `GET /cuentas`, `GET /cuentas/{id}`, `PATCH /cuentas/{id}/saldo`;
`ms-movimientos` `GET /movimientos/cuenta/{id}`, `POST /movimientos`; `ms-transacciones` `GET /transacciones`, `GET /transacciones/recientes`.

Las respuestas de error usan el formato `{ "timestamp", "status", "error" }`.
