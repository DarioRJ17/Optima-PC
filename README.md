# OptimaPC

Proyecto de TFG: plataforma web para la recomendación y gestión de componentes de PC, desarrollada con **Spring Boot (backend)** y **React + Vite (frontend)**.

---

## 1. Requisitos previos

Instalar en este orden:

### Herramientas comunes

- **Git**
- **Java JDK 21**
- **Node.js (LTS) + npm**
- **Visual Studio Code** (opcional, pero recomendado)

### Backend

- **PostgreSQL 17.x** (o 16.x)  
  - Crear una base de datos local, por ejemplo: `optimapc_db`
  - Usuario y contraseña configurados en PostgreSQL (por defecto suele ser `postgres`)

### Frontend

- Ningún requisito extra aparte de Node.js y npm.

---

## 2. Clonar el repositorio

```bash
git clone https://github.com/TU_USUARIO/optima-pc.git
cd optima-pc
```

La estructura del proyecto es:

```text
optima-pc/
  backend/
  frontend/
```

---

## 3. Configuración del backend (Spring Boot)

### 3.1. Variables de entorno (recomendado)

Definir estas variables de entorno en el sistema (Windows / Linux / macOS):

- `DB_USERNAME` → usuario de PostgreSQL  
- `DB_PASSWORD` → contraseña de ese usuario  

Ejemplo en Windows (PowerShell):

```powershell
setx DB_USERNAME "postgres"
setx DB_PASSWORD "tu_contraseña"
```

### 3.2. Archivo `backend/src/main/resources/application.properties`

El backend está configurado para una base de datos local PostgreSQL:

```properties
spring.application.name=backend

spring.datasource.url=jdbc:postgresql://localhost:5432/optimapc_db
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

Si se usa otro nombre de base de datos, puerto u host, adaptar la URL.

---

## 4. Arrancar el backend

Desde la carpeta `backend`:

```bash
cd backend
./mvnw spring-boot:run      # Linux / macOS
```

En Windows (PowerShell o CMD):

```bash
cd backend
mvnw.cmd spring-boot:run
```

El backend se expone por defecto en:

```text
http://localhost:8080
```

---

## 5. Arrancar el frontend (React + Vite)

Desde la carpeta `frontend`:

### 5.1. Instalar dependencias

```bash
cd frontend
npm install
```

### 5.2. Iniciar el servidor de desarrollo

```bash
npm run dev
```

Por defecto Vite arranca en una URL similar a:

```text
http://localhost:5173
```

---

## 6. Flujo básico de desarrollo

1. Abrir dos terminales:
   - Terminal 1 → `backend` → `mvnw spring-boot:run`
   - Terminal 2 → `frontend` → `npm run dev`
2. Acceder al frontend en el navegador (`http://localhost:5173`).
3. El frontend se comunica con el backend en `http://localhost:8080` (endpoints REST).

---

## 7. Notas adicionales

- El proyecto usa:
  - **Spring Web, Spring Data JPA, Spring Security, PostgreSQL Driver, Lombok, Validation y Spring Boot DevTools** en el backend.
  - **React + TypeScript + Vite** en el frontend.
- Para ejecutar los tests del backend:

```bash
cd backend
./mvnw test     # o mvnw.cmd test en Windows
```

- Para construir el frontend para producción:

```bash
cd frontend
npm run build
```