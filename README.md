# AI Resume Analyzer & Interview Coach

A production-grade, full-stack platform that helps job seekers **analyze resumes against job descriptions, detect skill gaps, get AI improvement suggestions, generate interview questions, and practice mock interviews with an AI interviewer**.

Built with an enterprise architecture: **Java 21 / Spring Boot 3**, **PostgreSQL**, **React 19 / Vite**, JWT auth, and a pluggable AI provider layer (OpenAI, Gemini, or an offline Mock provider so it runs with **zero API keys**).

---

## ✨ Features

| Module | Description |
| --- | --- |
| **Authentication** | Register, login, logout, JWT access + rotating refresh tokens, forgot/reset password, role-based access (Candidate / Admin). |
| **Resume Upload & Parsing** | PDF upload (validated, ≤10MB), text extraction via Apache PDFBox, structured field extraction (skills, education, experience…) with AI + regex fallback. |
| **Job Descriptions** | Paste a JD; AI structures it into required/preferred skills and keywords. |
| **Skill-Gap Analysis** | Resume vs. JD comparison → ATS match score, sub-scores (skills/experience/education/keywords), missing skills, strengths, weaknesses, recommendations. |
| **Interview Question Generator** | 20 tailored questions across Technical / Behavioral / HR / System-Design with difficulty levels. |
| **AI Mock Interview** | ChatGPT-style interviewer: asks one question at a time, scores each answer, produces final communication/technical/confidence/overall scores + improvement areas. Full transcript stored. |
| **Dashboard Analytics** | Recharts visualizations: resume & interview score trends, skill distribution, missing-skill frequency. |
| **Admin Panel** | Platform metrics + user management (list, delete). |
| **Profile** | View/update profile. |

## 🏗️ Tech Stack

**Backend:** Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA, JWT (jjwt), Apache PDFBox, Flyway, MapStruct, springdoc-openapi, Maven.
**Frontend:** React 19, Vite 6, React Router, TanStack Query, Axios, React Hook Form, Tailwind CSS, Recharts, Framer Motion, react-hot-toast.
**Database:** PostgreSQL 16.
**AI:** Strategy-pattern provider abstraction — `MockAiProvider` (default, offline), `OpenAiProvider`, `GeminiProvider`.

## 📁 Repository Layout

```
Resume Analyzer/
├── backend/        # Spring Boot API (feature-based packages)
├── frontend/       # React + Vite SPA (feature-based structure)
├── docs/           # ERD, architecture, API reference
├── docker-compose.yml
├── .env.example
└── README.md
```

Backend packages follow a **feature-first** layout (`auth`, `user`, `resume`, `job`, `analysis`, `interview`, `admin`, `dashboard`, `ai`, `storage`, `security`, `config`, `common`), each split into `domain / repository / service / controller / dto / mapper`.

## 🚀 Quick Start (Docker — recommended)

Requires Docker + Docker Compose. Runs DB + backend + frontend with the **offline Mock AI provider** (no keys needed).

```bash
cp .env.example .env          # adjust secrets
docker compose up --build
```

- Frontend:  http://localhost:8081
- API:       http://localhost:8080
- Swagger:   http://localhost:8080/swagger-ui.html
- Default admin: `admin@resume-analyzer.dev` / `Admin@12345` (change it!)

## 🛠️ Local Development

### Backend
Requires JDK 21 and PostgreSQL running locally (or `docker compose up db`).

```bash
cd backend
# Either install Maven, or use Docker to build (see Dockerfile).
mvn spring-boot:run
```

**Run without PostgreSQL (zero-dependency dev mode):** the `h2` profile uses a local
file-based H2 database — no Postgres, no Docker needed:

```bash
cd backend
SPRING_PROFILES_ACTIVE=h2 java -jar target/resume-analyzer-api.jar --server.port=8090
# (data persists in backend/data/). If port 8080 is free you can omit --server.port.
```

If your 8080 is occupied (e.g. by XAMPP/WAMP Apache), run the backend on another port
(like 8090 above) and point the frontend dev proxy at it via `frontend/.env`:
`VITE_API_PROXY_TARGET=http://localhost:8090`, then restart `npm run dev`.

Environment variables (all have dev defaults — see `application.yml`):
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `AI_PROVIDER`, `OPENAI_API_KEY`, `GEMINI_API_KEY`.

### Frontend
Requires Node 20+.

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173 (proxies /api → http://localhost:8080)
```

## 🤖 Switching AI Providers

The active provider is chosen by `AI_PROVIDER` (`mock` | `openai` | `gemini`) and resolved at runtime by `AiProviderResolver` — **no code changes required**.

```bash
AI_PROVIDER=openai
OPENAI_API_KEY=sk-...
# or
AI_PROVIDER=gemini
GEMINI_API_KEY=...
```

The `mock` provider implements every AI capability with deterministic heuristics, so the whole product is fully demoable offline and in CI.

## 🔌 API Overview

All responses use a uniform envelope: `{ success, message, data, error, timestamp }`.

| Area | Endpoints |
| --- | --- |
| Auth | `POST /api/auth/{register,login,refresh,logout,forgot-password,reset-password}` |
| Profile | `GET /api/profile`, `PUT /api/profile` |
| Resume | `POST /api/resume/upload`, `GET /api/resume`, `GET /api/resume/{id}`, `DELETE /api/resume/{id}` |
| Job | `POST /api/job`, `GET /api/job`, `GET /api/job/{id}` |
| Analysis | `POST /api/analysis/run`, `GET /api/analysis/{id}`, `GET /api/analysis` |
| Interview | `POST /api/interview/{questions,start,message}`, `POST /api/interview/{id}/complete`, `GET /api/interview/{id}`, `GET /api/interview/history` |
| Dashboard | `GET /api/dashboard` |
| Admin | `GET /api/admin/metrics`, `GET /api/admin/users`, `DELETE /api/admin/users/{id}` |

Full, interactive contract: **Swagger UI** at `/swagger-ui.html`. See [docs/API.md](docs/API.md) for examples.

## 🧪 Testing

```bash
cd backend && mvn test
```

Unit tests cover the AI heuristics and JSON parsing. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the recommended Testcontainers setup for full integration tests.

## 📐 Architecture & Data Model

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the system design, the **Strategy pattern** AI layer, the storage abstraction (local → S3), and the full **ERD**.

## 🔐 Security Notes

- Passwords hashed with BCrypt; refresh & reset tokens stored only as SHA-256 hashes.
- Stateless JWT auth; short-lived access tokens + rotating refresh tokens with logout revocation.
- Method- and URL-level role checks; ownership-scoped queries prevent cross-user data access.
- **Change `JWT_SECRET` and the default admin password before any shared deployment.**

## 📦 Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md).

## 📄 License

MIT.
