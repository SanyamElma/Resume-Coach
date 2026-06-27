# Deployment Guide

## 1. Prerequisites

- Docker + Docker Compose (simplest path), **or** JDK 21 + Maven + Node 20 + PostgreSQL 16 for manual deploys.

## 2. Configuration

Copy `.env.example` → `.env` and set, at minimum:

| Variable | Notes |
| --- | --- |
| `JWT_SECRET` | Base64 256-bit secret. `openssl rand -base64 48`. **Required in prod.** |
| `DB_PASSWORD` | Strong DB password. |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Seeded admin — change immediately. |
| `CORS_ALLOWED_ORIGINS` | Public URL(s) of the frontend. |
| `AI_PROVIDER` | `mock` (default), `openai`, or `gemini`. |
| `OPENAI_API_KEY` / `GEMINI_API_KEY` | Required only for the matching provider. |

## 3. Deploy to Render (one-click Blueprint)

The repo ships a [`render.yaml`](render.yaml) Blueprint that provisions **all three**
components and wires them together automatically:

1. Push the repo to GitHub (already done: `SanyamElma/Resume-Coach`).
2. Sign in at <https://dashboard.render.com> → **New** → **Blueprint**.
3. Connect the `Resume-Coach` repository. Render detects `render.yaml`.
4. Click **Apply**. Render creates:
   - `resume-coach-db` — managed PostgreSQL (free).
   - `resume-coach-api` — Spring Boot API built from `backend/Dockerfile`; DB creds,
     a generated `JWT_SECRET`, and CORS are injected automatically.
   - `resume-coach-frontend` — React static site; its `VITE_API_BASE_URL` is wired to
     the API service automatically.
5. First build takes a few minutes (Maven + Docker). When the API shows **Live**, open
   the frontend URL (e.g. `https://resume-coach-frontend.onrender.com`).

**After first deploy:**
- Change `ADMIN_PASSWORD` on the `resume-coach-api` service → Environment, then redeploy.
- To use a real LLM: set `AI_PROVIDER=openai` (or `gemini`) and add `OPENAI_API_KEY` /
  `GEMINI_API_KEY` in the same Environment tab.

**Free-tier caveats:**
- The API **sleeps after ~15 min idle**; the first request after that cold-starts in ~50s.
- The container filesystem is **ephemeral** — uploaded resume *files* are lost on restart
  (all relational data persists in Postgres). Attach a paid Render Disk, or migrate to S3
  (§8), for durable file storage.
- Render's free PostgreSQL is time-limited; upgrade the DB plan for anything long-lived.

## 4. Deploy with Docker Compose

```bash
docker compose up --build -d
docker compose logs -f backend
```

Services:
- `db` — PostgreSQL with a named volume (`db-data`).
- `backend` — built from `backend/Dockerfile` (multi-stage Maven build → JRE), Flyway migrates on boot, runs as non-root, health-checked at `/actuator/health`.
- `frontend` — built static bundle served by nginx, proxying `/api` → `backend:8080`.

Uploaded resumes persist in the `resume-storage` volume.

## 5. Manual / VM Deploy

**Database**
```sql
CREATE DATABASE resume_analyzer;
CREATE USER resume_user WITH PASSWORD '...';
GRANT ALL PRIVILEGES ON DATABASE resume_analyzer TO resume_user;
```

**Backend**
```bash
cd backend
mvn clean package -DskipTests
SPRING_PROFILES_ACTIVE=prod DB_URL=jdbc:postgresql://HOST:5432/resume_analyzer \
DB_USERNAME=resume_user DB_PASSWORD=... JWT_SECRET=... \
java -jar target/resume-analyzer-api.jar
```

**Frontend**
```bash
cd frontend
VITE_API_BASE_URL=https://api.yourdomain.com npm run build
# serve ./dist behind nginx / a CDN
```

## 6. Production Hardening Checklist

- [ ] Rotate `JWT_SECRET`; never use the shipped default.
- [ ] Change the seeded admin password.
- [ ] Terminate TLS at a load balancer / reverse proxy.
- [ ] Restrict `CORS_ALLOWED_ORIGINS` to known frontends.
- [ ] Wire a real `EmailService` for password-reset tokens (currently logged in dev — see `AuthService#forgotPassword`).
- [ ] Migrate storage to S3 by implementing `StorageService` and setting `STORAGE_PROVIDER=s3`.
- [ ] Add DB backups for the `db-data` volume / managed Postgres.
- [ ] Set resource limits and run behind an autoscaler; the API is stateless and scales horizontally.
- [ ] Add Testcontainers integration tests to CI (see ARCHITECTURE.md §7).

## 7. Observability

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Swagger: `GET /swagger-ui.html`

## 8. AWS S3 Migration (future)

`StorageService` already abstracts file IO. To migrate:
1. Add the AWS SDK v2 `s3` dependency.
2. Implement `S3StorageService implements StorageService` (`@ConditionalOnProperty app.storage.provider=s3`).
3. Set `STORAGE_PROVIDER=s3` + bucket/region/credentials env vars.
No controller/service changes are needed — keys remain opaque.
