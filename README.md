# OptimaPC

Proyecto de TFG: plataforma web para la recomendación y gestión de componentes de PC, desarrollada con **Spring Boot (backend)** y **React + Vite (frontend)**.

---

## Ejecución rápida con Docker (recomendado para evaluadores)

Este es el método más sencillo para poner en marcha el proyecto sin instalar dependencias manualmente.

### Requisito único

- **Docker Desktop** → https://www.docker.com/products/docker-desktop/

### Pasos

```bash
git clone https://github.com/TU_USUARIO/optima-pc.git
cd optima-pc
docker-compose up --build
```

Docker descargará y construirá automáticamente todos los servicios:

| Servicio | URL |
|---|---|
| Frontend (React) | http://localhost:5173 |
| Backend (Spring Boot) | http://localhost:8080 |

> **Primera ejecución:** el asistente de chat usa el modelo `qwen2.5:7b` (~4,5 GB). La descarga se hace automáticamente al arrancar y puede tardar varios minutos según la conexión. Las siguientes ejecuciones lo cargan desde el volumen local sin volver a descargar.

Para detener todos los servicios:

```bash
docker-compose down
```

Para eliminar también los datos persistidos (base de datos y modelos):

```bash
docker-compose down -v
```

---

## Desarrollo local (configuración manual)

Esta sección es para desarrolladores que prefieren ejecutar los servicios directamente en su máquina.

### 1. Requisitos previos

- **Git**
- **Java JDK 21**
- **Node.js (LTS) + npm**
- **PostgreSQL 16.x / 17.x**
  - Crear una base de datos: `optimapc_db`
- **Ollama** → https://ollama.com/download
  - Una vez instalado, descargar el modelo:
    ```bash
    ollama pull qwen2.5:7b
    ```

### 2. Clonar el repositorio

```bash
git clone https://github.com/TU_USUARIO/optima-pc.git
cd optima-pc
```

Estructura del proyecto:

```text
optima-pc/
  backend/
  frontend/
```

### 3. Configuración del backend

Crear el archivo `backend/.env` con las siguientes variables (o definirlas como variables de entorno del sistema):

```properties
DB_USERNAME=postgres
DB_PASSWORD=tu_contraseña
JWT_SECRET=una-clave-larga-de-al-menos-32-caracteres
MAIL_USERNAME=tu_correo@gmail.com
MAIL_PASSWORD=tu_contraseña_de_aplicacion
MAIL_FROM_ADDRESS=tu_correo@gmail.com
FRONTEND_BASE_URL=http://localhost:5173
OLLAMA_URL=http://localhost:11434/api/chat
OLLAMA_MODEL=qwen2.5:7b
```

> Para la funcionalidad de recuperación de contraseña se necesita una cuenta Gmail con verificación en dos pasos y una contraseña de aplicación generada desde la configuración de Google. El resto de funcionalidades no requieren configuración de correo.

### 4. Arrancar Ollama

Asegúrate de que Ollama está en ejecución antes de arrancar el backend:

```bash
ollama serve
```

### 5. Arrancar el backend

Desde la carpeta `backend`:

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

El backend se expone en: `http://localhost:8080`

### 6. Arrancar el frontend

Desde la carpeta `frontend`:

```bash
npm install
npm run dev
```

El frontend se expone en: `http://localhost:5173`

---

## Notas adicionales

- Stack del proyecto:
  - **Backend:** Spring Boot 4, Spring Data JPA, Spring Security, PostgreSQL, Lombok, Validation
  - **Frontend:** React + TypeScript + Vite
  - **Chat:** Ollama con modelo `qwen2.5:7b` (local)

- Ejecutar tests del backend:
  ```bash
  cd backend
  ./mvnw test     # o mvnw.cmd test en Windows
  ```

- Construir el frontend para producción:
  ```bash
  cd frontend
  npm run build
  ```
