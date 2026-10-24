# TaskFlow API ---

A REST backend for task tracking and team collaboration, built with Java 17 and Spring Boot 3.
Users can register, create projects (teams), invite people, create and assign tasks, filter and search them,
and collaborate through comments and file attachments.

## Contents

- [Tech stack](#tech-stack)
- [Features and user-story coverage](#features-and-user-story-coverage)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [API overview](#api-overview)
- [Example walkthrough](#example-walkthrough-curl)
- [Project structure](#project-structure)
- [Design decisions](#design-decisions)
- [Running the tests](#running-the-tests)
- [Known limitations and next steps](#known-limitations-and-next-steps)

## Tech stack

| Area            | Choice                                                         |
|-----------------|----------------------------------------------------------------|
| Language        | Java 17                                                        |
| Framework       | Spring Boot 3.2 (Web, Data JPA, Security, Validation)          |
| Build tool      | Maven                                                          |
| Database        | H2 (default, zero setup) or PostgreSQL (`postgres` profile)    |
| Auth            | JWT (jjwt) with BCrypt password hashing                        |
| API docs        | springdoc-openapi (Swagger UI)                                 |
| Tests           | JUnit 5, Spring Boot Test, MockMvc                             |

## Features and user-story coverage

| # | User story                                   | How it is covered                                              |
|---|----------------------------------------------|----------------------------------------------------------------|
| 1 | Create an account                            | `POST /api/auth/register`                                      |
| 2 | Log in securely                              | `POST /api/auth/login` returns a JWT                           |
| 3 | View and update my profile                   | `GET/PUT /api/users/me`, `PUT /api/users/me/password`          |
| 4 | Create a task (title, description, due date) | `POST /api/projects/{id}/tasks`                                |
| 5 | List tasks assigned to me                    | `GET /api/tasks/mine`                                          |
| 6 | Mark a task completed                        | `PATCH /api/tasks/{id}/status` with `COMPLETED`                |
| 7 | Assign a task to a team member               | `PATCH /api/tasks/{id}/assignee` (or `assigneeId` on create)   |
| 8 | Filter by status                             | `?status=OPEN` / `IN_PROGRESS` / `COMPLETED`                   |
| 9 | Search by title or description               | `?q=text` (case-insensitive)                                   |
| 10| Comments and attachments                     | `/api/tasks/{id}/comments`, `/api/tasks/{id}/attachments`      |
| 11| Create a team/project and invite people      | `POST /api/projects`, invite code + `POST .../members`         |
| 12| Log out                                      | `POST /api/auth/logout` (token is revoked server-side)         |

The optional extensions (real-time notifications and AI-generated descriptions) are intentionally not part of this version.

## Getting started

### Prerequisites

- JDK 17 or newer
- Maven 3.9+
- (Optional) Docker, if you want to run PostgreSQL

### Run with the built-in H2 database

```bash
mvn spring-boot:run
```

The API is available at `http://localhost:8080`. Data is stored in `./data/` and uploads in `./uploads/`
(both are git-ignored), so it survives restarts.

Interactive documentation: <http://localhost:8080/swagger-ui.html>
(click **Authorize** and paste the token returned by login/register).

### Run with PostgreSQL

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Connection details can be overridden with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

### Build a jar

```bash
mvn clean package
java -jar target/taskflow-api-1.0.0.jar
```

## Configuration

| Property / env var                        | Default                      | Purpose                                      |
|-------------------------------------------|------------------------------|----------------------------------------------|
| `JWT_SECRET` (`app.jwt.secret`)           | development-only value       | HMAC signing key, at least 32 characters     |
| `app.jwt.expiration-ms`                   | `86400000` (24 h)            | Token lifetime                               |
| `UPLOAD_DIR` (`app.storage.upload-dir`)   | `./uploads`                  | Where attachments are stored                 |
| `spring.servlet.multipart.max-file-size`  | `10MB`                       | Upload size limit                            |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`    | local Postgres               | Only used with the `postgres` profile        |

Always set your own `JWT_SECRET` outside of local development.

## API overview

All endpoints except register/login require the header `Authorization: Bearer <token>`.

### Auth and profile

| Method | Path                      | Description                          |
|--------|---------------------------|--------------------------------------|
| POST   | `/api/auth/register`      | Create an account, returns a token   |
| POST   | `/api/auth/login`         | Log in, returns a token              |
| POST   | `/api/auth/logout`        | Revoke the current token             |
| GET    | `/api/users/me`           | Current user's profile               |
| PUT    | `/api/users/me`           | Update full name                     |
| PUT    | `/api/users/me/password`  | Change password                      |

### Projects (teams)

| Method | Path                               | Description                                     |
|--------|------------------------------------|-------------------------------------------------|
| POST   | `/api/projects`                    | Create a project (caller becomes owner)         |
| GET    | `/api/projects`                    | Projects I belong to                            |
| GET    | `/api/projects/{id}`               | Project details (invite code shown to owner)    |
| POST   | `/api/projects/join`               | Join using an invite code                       |
| GET    | `/api/projects/{id}/members`       | List members                                    |
| POST   | `/api/projects/{id}/members`       | Owner adds a registered user by email           |

### Tasks

| Method | Path                                   | Description                                       |
|--------|----------------------------------------|---------------------------------------------------|
| POST   | `/api/projects/{projectId}/tasks`      | Create a task                                     |
| GET    | `/api/projects/{projectId}/tasks`      | List/filter/search/sort tasks of a project        |
| GET    | `/api/tasks/mine`                      | Tasks assigned to me                              |
| GET    | `/api/tasks/{id}`                      | Task details                                      |
| PUT    | `/api/tasks/{id}`                      | Edit title, description, due date, priority       |
| PATCH  | `/api/tasks/{id}/status`               | Change status                                     |
| PATCH  | `/api/tasks/{id}/assignee`             | Assign (or un-assign with `null`)                 |
| DELETE | `/api/tasks/{id}`                      | Delete task with its comments and attachments     |

**Query parameters for list endpoints**

| Parameter    | Applies to        | Notes                                                       |
|--------------|-------------------|-------------------------------------------------------------|
| `status`     | both              | `OPEN`, `IN_PROGRESS`, `COMPLETED`                          |
| `q`          | both              | Matches title or description, case-insensitive              |
| `assigneeId` | project list      | Only tasks assigned to this user                            |
| `projectId`  | `/tasks/mine`     | Limit "my tasks" to one project                             |
| `sortBy`     | both              | `createdAt`, `updatedAt`, `dueDate`, `title`                |
| `direction`  | both              | `asc` or `desc`                                             |
| `page`,`size`| both              | Zero-based page, size capped at 100                         |

### Comments and attachments

| Method | Path                                            | Description                  |
|--------|-------------------------------------------------|------------------------------|
| POST   | `/api/tasks/{taskId}/comments`                  | Add a comment                |
| GET    | `/api/tasks/{taskId}/comments`                  | List comments (oldest first) |
| POST   | `/api/tasks/{taskId}/attachments`               | Upload (`multipart`, field `file`) |
| GET    | `/api/tasks/{taskId}/attachments`               | List attachment metadata     |
| GET    | `/api/tasks/{taskId}/attachments/{id}`          | Download the file            |
| DELETE | `/api/tasks/{taskId}/attachments/{id}`          | Delete (uploader or project owner) |

### Error format

Every error uses the same JSON shape:

```json
{
  "timestamp": "2026-10-24T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": { "title": "Title is required" }
}
```

Status codes used: `400` validation, `401` missing/invalid token or wrong credentials, `403` not allowed,
`404` not found, `409` duplicates, `413` file too large.

## Example walkthrough (curl)

```bash
# 1. Register (the response contains the token)
curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"fullName":"Asha Patel","email":"asha@example.com","password":"Passw0rd!23"}'

TOKEN=<paste token here>

# 2. Create a project
curl -s -X POST localhost:8080/api/projects -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"name":"Website revamp","description":"Q4 work"}'

# 3. Create a task in project 1
curl -s -X POST localhost:8080/api/projects/1/tasks -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Design landing page","description":"Hero + footer","dueDate":"2026-12-01","priority":"HIGH"}'

# 4. List open tasks that mention "landing", newest due date first
curl -s "localhost:8080/api/projects/1/tasks?status=OPEN&q=landing&sortBy=dueDate&direction=desc" \
  -H "Authorization: Bearer $TOKEN"

# 5. Mark it completed
curl -s -X PATCH localhost:8080/api/tasks/1/status -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"status":"COMPLETED"}'

# 6. Upload an attachment
curl -s -X POST localhost:8080/api/tasks/1/attachments -H "Authorization: Bearer $TOKEN" \
  -F "file=@./mockup.png"

# 7. Log out
curl -s -X POST localhost:8080/api/auth/logout -H "Authorization: Bearer $TOKEN"
```

## Project structure

```
src/main/java/com/taskflow
├── config        Spring Security and OpenAPI configuration
├── controller    REST controllers (thin: parse request, call a service)
├── dto           Request/response records with validation annotations
├── exception     Custom exceptions and the global error handler
├── model         JPA entities and enums
├── repository    Spring Data repositories (+ Specifications for task filters)
├── security      JWT service, auth filter, user lookup, token blacklist
└── service       Business rules and authorization checks
```

## Design decisions

- **Projects are teams.** A project has an owner and members; every task belongs to one project, and only
  members can see or touch its tasks, comments and attachments. People join either by invite code or because the
  owner added them by email.
- **Who can change what.** Any member can view and comment. Editing a task, changing its status or reassigning it
  is limited to the creator, the current assignee and the project owner. Deleting a task needs the creator or the
  owner. Assignees must be members of the task's project.
- **JWT with server-side logout.** Tokens carry the user's email and a unique id. Logout stores that id in a small
  table and the auth filter rejects it afterwards; a scheduled job cleans up rows once the token would have
  expired anyway. This keeps the API stateless while still making logout real.
- **Passwords** are hashed with BCrypt and never returned by the API. Emails are normalised to lower case.
- **Dynamic filtering** uses JPA Specifications, so status, assignee and text filters can be combined freely
  while paging and sorting are handled by the database. Sort fields are whitelisted.
- **DTO records** keep entities out of the API, and validation lives on the DTOs.
- **Attachments** are stored on disk under random UUID names; the original file name is kept only as metadata,
  so a crafted file name can never influence where a file is written.
- **Consistent errors.** One `@RestControllerAdvice` maps everything to the same JSON structure.

## Running the tests

```bash
mvn clean verify
```

- `JwtServiceTest` covers token creation, tampering, expiry and wrong-key rejection.
- `TaskFlowApiTest` runs the main user stories end to end with MockMvc on an in-memory database: register, create
  and join a project, create/assign/search/filter/complete a task, comment, upload and download an attachment,
  permission checks, validation errors and logout.

## Known limitations and next steps

- Real-time notifications (WebSocket/SSE) and AI-assisted descriptions are not implemented yet.
- Schema is created with `ddl-auto: update`, which is convenient locally; a production setup should use
  Flyway or Liquibase migrations.
- Attachments live on local disk; a cloud object store would be the next step for multi-instance deployments.
- No refresh tokens or rate limiting on the login endpoint.
- Project owners cannot yet remove members or transfer ownership.
