<h3 align="center">IT Helpdesk System</h3>

<p align="center">
    <strong>
    Corporate IT incident and service request management system.
    </strong>
</p>
<p align="center">
    <img src="https://img.shields.io/badge/Status-In_Development-yellow?style=flat-square" alt="Status">
    <img src="https://img.shields.io/badge/Java-21-blue?style=flat-square&logo=openjdk" alt="Java 21">
    <img src="https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?style=flat-square&logo=spring" alt="Spring Boot">
    <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql" alt="PostgreSQL">
    <img src="https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react" alt="React">
</p>
<hr>

## 📌 About

A coursework project aimed at developing an internal Service Desk / Helpdesk application for corporate IT support teams.

The system automates the intake of employee issues, facilitates transparent ticket assignment for support specialists, and tracks incident lifecycles through a strict state machine.

## 🛠 Tech Stack

* **Backend:** Java 21, Spring Boot 4.1 (Spring Web, Spring Data JPA, Spring Security, Validation)
* **Database:** PostgreSQL 16, Hibernate ORM
* **API Documentation:** Springdoc OpenAPI 3 (Swagger UI)
* **Frontend:** React 18, TypeScript, Vite, Tailwind CSS

## 🚀 Development

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Run backend (REST API + Swagger UI on http://localhost:8080/swagger-ui.html)
./gradlew bootRun

# 3. Run frontend (SPA on http://localhost:5173, proxies /api to :8080)
cd frontend && npm ci && npm run dev
```

> Upgrading from an older checkout? Run `docker compose down --remove-orphans`
> once to drop the legacy named `ithelpdesk-postgres` container.

On first startup the backend seeds the `IT Service Desk` department, four default
categories and the admin account `admin@company.ru` / `Admin2024!`
(overridable via `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_FULL_NAME`).

## 📦 Production

```bash
# 1. Configure secrets (copy the template and replace every required value)
cp .env.example .env

# 2. Build images and start the whole stack (nginx + backend + postgres)
docker compose --env-file .env -f docker-compose.prod.yml up -d --build
```

The unprivileged nginx container serves the built SPA on port `8080`
(override with `PORT`) and proxies `/api`, Swagger UI and OpenAPI docs to the
backend container. PostgreSQL is only reachable from the internal network and
is never published to the host.

Required env vars (see `.env.example`):

| Variable | Description |
|:---|:---|
| `DB_PASSWORD` | PostgreSQL password, **required** |
| `JWT_SECRET` | HMAC secret, **required**, at least 32 bytes (`openssl rand -base64 48`) |
| `ADMIN_PASSWORD` | Initial admin password, **required** |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins for the API (default `http://localhost:8080`) |
| `ADMIN_EMAIL` / `ADMIN_FULL_NAME` | Initial admin identity (sensible defaults) |
| `PORT` | Public nginx port (default `8080`) |

Both images are built without secrets, run as non-root users and expose
container health checks that Compose uses to order startup.

## ✅ Quality Checks

```bash
./gradlew test                 # backend unit tests
cd frontend && npm run lint    # frontend linting
cd frontend && npm run build   # type-check + production bundle
```

## 📚 API

* Swagger UI: `http://localhost:8080/swagger-ui.html`
* OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 🔐 Security Notes

* JWT (HS256, 24 h) with user re-validation on every request.
* Login rate limiting (5 attempts / 5 min → `429`).
* Protected resources are masked as `404` for unauthenticated / unauthorized requests.
* Production containers run as non-root; PostgreSQL stays on an internal Docker network.
