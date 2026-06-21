# API Reference

Base URL: `http://localhost:8080`. All endpoints (except `/api/auth/**`) require an
`Authorization: Bearer <accessToken>` header. Every response uses the envelope:

```json
{ "success": true, "message": "OK", "data": { }, "error": null, "timestamp": "2026-06-21T10:00:00Z" }
```

Errors:

```json
{ "success": false, "message": "Invalid email or password",
  "error": { "code": "INVALID_CREDENTIALS", "message": "...", "path": "/api/auth/login" },
  "timestamp": "..." }
```

## Authentication

### Register — `POST /api/auth/register`
```json
{ "name": "Jane Doe", "email": "jane@example.com", "password": "Sup3rSecret!" }
```
Returns `{ accessToken, refreshToken, tokenType, expiresInSeconds, user }`.

### Login — `POST /api/auth/login`
```json
{ "email": "jane@example.com", "password": "Sup3rSecret!" }
```

### Refresh — `POST /api/auth/refresh`
```json
{ "refreshToken": "<opaque-refresh-token>" }
```
Rotates the refresh token and returns a fresh token pair.

### Logout — `POST /api/auth/logout` · Forgot — `POST /api/auth/forgot-password` · Reset — `POST /api/auth/reset-password`

## Resume

| Method | Path | Body |
| --- | --- | --- |
| POST | `/api/resume/upload` | `multipart/form-data`: `file` (PDF), `name` (optional) |
| GET | `/api/resume?page=0&size=10` | — |
| GET | `/api/resume/{id}` | — |
| DELETE | `/api/resume/{id}` | — |

## Job Description

`POST /api/job`
```json
{ "title": "Senior Java Engineer", "company": "Acme", "description": "We are looking for..." }
```
`GET /api/job?page=0&size=10` · `GET /api/job/{id}`

## Analysis

`POST /api/analysis/run`
```json
{ "resumeId": "<uuid>", "jobDescriptionId": "<uuid>" }
```
Response `data`:
```json
{
  "id": "<uuid>", "matchPercentage": 85,
  "skillMatchScore": 88, "experienceMatchScore": 80, "educationMatchScore": 75, "keywordMatchScore": 90,
  "missingSkills": ["kubernetes"], "strengths": ["..."], "weaknesses": ["..."], "recommendations": ["..."]
}
```
`GET /api/analysis/{id}` · `GET /api/analysis?page=0&size=10`

## Interview

`POST /api/interview/questions`
```json
{ "resumeId": "<uuid|null>", "jobDescriptionId": "<uuid|null>", "count": 20 }
```
→ array of `{ category, difficulty, question }`.

`POST /api/interview/start`
```json
{ "resumeId": "<uuid|null>", "jobDescriptionId": "<uuid|null>" }
```
→ session with the interviewer's opening message.

`POST /api/interview/message`
```json
{ "sessionId": "<uuid>", "message": "My answer..." }
```
→ updated session (your scored answer + the next AI question).

`POST /api/interview/{id}/complete` → final scores + feedback.
`GET /api/interview/{id}` · `GET /api/interview/history?page=0&size=10`

## Dashboard — `GET /api/dashboard`
Returns totals plus `resumeScoreTrend`, `interviewScoreTrend`, `skillDistribution`, `missingSkillsFrequency`.

## Admin (ROLE_ADMIN)
`GET /api/admin/metrics` · `GET /api/admin/users?page=0&size=20` · `DELETE /api/admin/users/{id}`
