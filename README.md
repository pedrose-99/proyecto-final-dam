# SmartCart

Comparador de precios de supermercados. Aplicacion web que permite buscar, comparar y hacer seguimiento de precios de productos de diferentes supermercados (Mercadona, Alcampo, Carrefour, Dia), optimizar listas de la compra y recibir alertas de precio.

## Stack tecnologico

| Componente | Tecnologia | Version |
|---|---|---|
| Backend | Spring Boot (Java) | 3.5.10 (Java 21) |
| Frontend | Angular + Angular Material | 21 |
| Base de datos | PostgreSQL | 16 |
| API | GraphQL + REST | - |
| Scraper auxiliar | FastAPI + Playwright (Python) | 3.11 |
| Mensajeria | Apache Kafka | 7.6.0 |
| Contenedores | Docker Compose | - |

## Arquitectura

```
                    ┌──────────────┐
                    │   Frontend   │ :4200
                    │   Angular    │
                    └──────┬───────┘
                           │ GraphQL / REST
                    ┌──────┴───────┐
                    │   Backend    │ :8081
                    │ Spring Boot  │
                    └──┬───┬───┬───┘
                       │   │   │
            ┌──────────┘   │   └──────────┐
            │              │              │
     ┌──────┴──────┐ ┌────┴─────┐ ┌──────┴──────┐
     │ PostgreSQL  │ │  Kafka   │ │   Scraper   │
     │             │ │          │ │   Python    │
     └─────────────┘ └──────────┘ └─────────────┘
          :5434         :9092          :8000
```

## Requisitos previos

- [Docker](https://docs.docker.com/get-docker/) y Docker Compose
- **Windows**: Docker Desktop
- **Linux**: Docker Engine + Docker Compose plugin

No es necesario instalar Java, Node.js ni PostgreSQL; todo se ejecuta dentro de contenedores Docker.

### Solo si quieres ejecutar sin Docker

- Java 21 (JDK)
- Node.js 20+ y npm
- PostgreSQL 16
- Python 3.11+ (para el scraper)
- Apache Kafka

> **Nota:** La primera ejecucion puede tardar entre 5 y 15 minutos, ya que Docker necesita descargar las imagenes base y compilar todos los servicios. Las siguientes ejecuciones seran mucho mas rapidas al usar la cache de Docker.

---

## Inicio rapido con Docker

### Linux / macOS

```bash
# Clonar el repositorio
git clone <url-del-repositorio>
cd proyecto-final-dam

# Levantar todos los servicios
docker compose up --build

# O en segundo plano
docker compose up --build -d
```

### Windows (con script .bat)

1. Asegurate de tener **Docker Desktop** iniciado.
2. Haz doble clic en `start.bat` o ejecutalo desde la terminal:

```cmd
start.bat
```

Se mostrara un menu con las siguientes opciones:

```
1. Iniciar todos los servicios
2. Iniciar en segundo plano (detached)
3. Parar todos los servicios
4. Reiniciar todos los servicios
5. Ver logs
6. Ver estado de los contenedores
7. Limpiar todo (para + elimina volumenes)
0. Salir
```

### Windows (sin script .bat)

```cmd
:: Levantar todos los servicios
docker compose up --build

:: O en segundo plano
docker compose up --build -d

:: Parar
docker compose down
```

---

## URLs de acceso

Una vez que todos los servicios esten levantados:

| Servicio | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend (REST) | http://localhost:8081 |
| GraphQL | http://localhost:8081/graphql |
| GraphiQL (playground) | http://localhost:8081/graphiql |
| Scraper Python | http://localhost:8000 |
| PostgreSQL | `localhost:5434` |
| Kafka | `localhost:9092` |

## Primeros pasos

La aplicacion no tiene cuentas creadas por defecto. Es necesario **registrarse** desde la pantalla de login para poder acceder.

Para acceder al **panel de administracion**, utiliza las siguientes credenciales:

| Campo | Valor |
|---|---|
| Email | `admin@admin.com` |
| Password | `pass` |

---

## Servicios Docker

| Servicio | Contenedor | Puerto | Descripcion |
|---|---|---|---|
| PostgreSQL | smartcart-db | 5434 | Base de datos principal |
| Scraper Python | smartcart-scraper-python | 8000 | Scraping con Playwright (Dia, Carrefour) |
| Backend | smartcart-backend | 8081 | API GraphQL + REST + scraping Java |
| Frontend | smartcart-frontend | 4200 | Aplicacion Angular con hot-reload |
| Zookeeper | zookeeper-1 | 2181 | Coordinacion para Kafka |
| Kafka | kafka-1 | 9092 | Broker de mensajeria |

### Orden de arranque

Docker Compose gestiona las dependencias automaticamente:

1. **PostgreSQL** y **Zookeeper** arrancan primero
2. **Kafka** espera a Zookeeper
3. **Scraper Python** arranca en paralelo
4. **Backend** espera a que PostgreSQL y Scraper Python esten sanos (healthcheck)
5. **Frontend** espera al Backend

---

## Ejecucion sin Docker (desarrollo local)

Si prefieres ejecutar los servicios directamente en tu maquina:

### 1. Base de datos

Instala PostgreSQL 16 y crea la base de datos:

```sql
CREATE DATABASE smartcart;
CREATE USER smartcart WITH PASSWORD 'smartcart123';
GRANT ALL PRIVILEGES ON DATABASE smartcart TO smartcart;
```

### 2. Kafka (opcional, necesario para funcionalidad de grupos)

Instala y arranca Zookeeper y Kafka en los puertos por defecto (2181 y 9092).

### 3. Scraper Python

```bash
cd scraper-python
pip install -r requirements.txt
playwright install chromium
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### 4. Backend

```bash
cd backend

# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

El backend se configura mediante el archivo `backend/.env`. Las variables principales son:

| Variable | Valor por defecto | Descripcion |
|---|---|---|
| `DB_HOST` | localhost | Host de PostgreSQL |
| `DB_PORT` | 5434 | Puerto de PostgreSQL |
| `DB_NAME` | smartcart | Nombre de la base de datos |
| `DB_USERNAME` | smartcart | Usuario de la base de datos |
| `DB_PASSWORD` | smartcart123 | Password de la base de datos |
| `SERVER_PORT` | 8080 | Puerto del backend |
| `JWT_SECRET` | (generado) | Clave secreta para tokens JWT |

### 5. Frontend

```bash
cd frontend
npm install --legacy-peer-deps
npm start
```

El frontend estara disponible en http://localhost:4200.

---

## Comandos utiles

```bash
# Ver logs de todos los servicios
docker compose logs -f

# Ver logs de un servicio especifico
docker compose logs -f backend
docker compose logs -f frontend

# Ver estado de los contenedores
docker compose ps

# Parar todos los servicios
docker compose down

# Parar y eliminar volumenes (borra datos de la BD)
docker compose down -v

# Reconstruir un servicio especifico
docker compose up --build backend

# Acceder a la base de datos
docker exec -it smartcart-db psql -U smartcart -d smartcart
```

---

## Estructura del proyecto

```
proyecto-final-dam/
├── backend/                 # Spring Boot 3.5.10, Java 21
│   ├── src/main/java/com/smartcart/smartcart/
│   │   ├── modules/
│   │   │   ├── auth/        # Login, registro, JWT
│   │   │   ├── user/        # Usuarios y roles
│   │   │   ├── product/     # Productos, precios, alertas
│   │   │   ├── category/    # Categorias
│   │   │   ├── store/       # Supermercados
│   │   │   ├── scraping/    # Scrapers (Mercadona, Alcampo, Carrefour)
│   │   │   ├── shoppinglist/# Listas de la compra + optimizador
│   │   │   ├── group/       # Grupos colaborativos (Kafka)
│   │   │   ├── favorite/    # Productos favoritos
│   │   │   └── notification/# Notificaciones
│   │   ├── config/          # Seguridad, WebSocket, DataInitializer
│   │   ├── security/        # JWT provider y filtro
│   │   ├── graphql/         # Resolvers GraphQL
│   │   └── common/          # DTOs base, excepciones, enums
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── graphql/         # Esquemas GraphQL (.graphqls)
│   ├── Dockerfile
│   ├── pom.xml
│   └── .env                 # Variables de entorno
│
├── frontend/                # Angular 21
│   ├── src/app/
│   │   ├── features/        # Modulos de funcionalidad
│   │   ├── shared/          # Componentes y servicios compartidos
│   │   └── core/            # Servicios core, guards, interceptors
│   ├── Dockerfile
│   ├── package.json
│   └── angular.json
│
├── scraper-python/          # FastAPI + Playwright
│   ├── app/
│   │   ├── main.py          # Endpoints del scraper
│   │   └── scrapers/        # Scrapers de Dia, Carrefour, Ahorramas
│   ├── Dockerfile
│   └── requirements.txt
│
├── docs/                    # Documentacion adicional
├── docker-compose.yml       # Orquestacion de todos los servicios
├── start.bat                # Script de inicio para Windows
└── README.md
```

---

## Funcionalidades principales

- **Comparador de precios**: Busca productos y compara precios entre supermercados
- **Scraping automatizado**: Recopilacion de productos y precios de Mercadona, Alcampo, Carrefour y Dia
- **Listas de la compra**: Crea listas y optimizalas por precio o por supermercado
- **Alertas de precio**: Recibe notificaciones cuando un producto baja de precio
- **Historial de precios**: Consulta la evolucion del precio de cualquier producto
- **Grupos colaborativos**: Comparte listas de la compra con otros usuarios (via Kafka)
- **Autenticacion JWT**: Registro, login y gestion de sesiones con tokens de acceso y refresco
