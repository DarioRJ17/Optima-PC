# OptimaPC

Proyecto de TFG: plataforma web para la recomendación y gestión de componentes de PC, desarrollada con **Spring Boot (backend)** y **React + Vite (frontend)**.

---

## Puesta en marcha (local)

### 1. Requisitos previos

- **Git** → https://git-scm.com/downloads
- **Java JDK 21** → https://adoptium.net/temurin/releases/?version=21
- **Node.js (LTS) + npm** → https://nodejs.org/
- **PostgreSQL 16.x / 17.x** → https://www.postgresql.org/download/
  - Crear una base de datos: `optimapc_db`
- **API key de Groq** (gratuita) → https://console.groq.com/keys
  - El asistente de chat usa la API de Groq (modelo `llama-3.1-8b-instant` por defecto).
- **Secreto para JWT** (lo generas tú, no se descarga de ningún sitio)
  - Sirve para **firmar los tokens JWT** que autentican a los usuarios. Debe ser una cadena aleatoria de **al menos 32 caracteres**. Para generar uno:
    ```bash
    openssl rand -base64 32
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
LLM_API_KEY=tu_api_key_de_groq
```

> El asistente de chat usa **Groq** (modelo `llama-3.1-8b-instant`). Sin `LLM_API_KEY` el chat no responderá, pero el resto de la aplicación funciona con normalidad. Opcionalmente pueden sobrescribirse `LLM_URL` y `LLM_MODEL`.

> Para la funcionalidad de recuperación de contraseña se necesita una cuenta Gmail con verificación en dos pasos y una contraseña de aplicación generada desde la configuración de Google. El resto de funcionalidades no requieren configuración de correo.

### 4. Arrancar el backend

Desde la carpeta `backend`:

```bash
# Linux / macOS / Windows
./mvnw spring-boot:run
```

El backend se expone en: `http://localhost:8080`

### 5. Arrancar el frontend

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
  - **Chat:** Groq con modelo `llama-3.1-8b-instant` (API en la nube, compatible con OpenAI)

- Ejecutar los tests del backend (genera además el informe de cobertura de JaCoCo):
  ```bash
  cd backend
  ./mvnw test          # mvnw.cmd test en Windows (cmd)
  ```
  Tras la ejecución, el informe de cobertura queda en `backend/target/site/jacoco/index.html`.
