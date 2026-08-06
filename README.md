# 106-StackSmashers

## Problem Statement

Financial systems process a large number of transactions every day. Manual monitoring is slow, error-prone, and cannot detect suspicious patterns in real time.

## What This Application Solves

This project provides a Transaction Monitoring System (TMS) that helps teams:

- Ingest and view transactions
- Detect suspicious activity using configurable monitoring rules
- Generate alerts for investigation
- Track investigations and reporting from a single dashboard

## How It Solves the Problem

The app uses a full-stack architecture:

- Backend: Spring Boot API with MySQL storage
- Frontend: React dashboard for operations and investigation workflows
- Rule Engine: Applies detection rules (high amount, rapid transactions, restricted country, etc.)

Default seed data includes an admin account and baseline monitoring rules so the system is usable immediately after startup.

## Tech Stack

- Java 17, Spring Boot 3
- MySQL 8
- React + Vite + Tailwind CSS
- Docker and Docker Compose

## Project Structure

- backend: Spring Boot services, controllers, repositories, security
- frontend: React UI, pages, API clients
- mysql/init: Database schema initialization scripts
- docker-compose.yml: Local container orchestration
- Dockerfile: Multi-stage backend and frontend container build

## Prerequisites (Local Run)

- Java 17
- Node.js 20+ and npm
- MySQL 8

## Environment and Configuration

Backend reads these variables (with defaults in application.properties):

- DB_HOST (default: localhost)
- DB_PORT (default: 3306)
- DB_NAME (default: tms_db)
- DB_USER (default: tms_user)
- DB_PASSWORD (default: tms_password)
- JWT_SECRET (default is set for development)
- FRONTEND_URL (default: http://localhost:3000)

Backend base URL:

- http://localhost:8080/api

Frontend dev URL:

- http://localhost:3000

## How to Run (Local, Without Docker)

### 1) Start MySQL

Create a database and user matching your backend config (or use the defaults above).

### 2) Run Backend

From the backend folder:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend starts at:

- http://localhost:8080/api

Health endpoint:

- http://localhost:8080/api/actuator/health

### 3) Run Frontend

From the frontend folder:

```bash
npm install
npm run dev
```

The frontend will run at:

- http://localhost:3000

Note: In development, Vite proxies /api calls to http://localhost:8080.

## How to Run (Docker Compose)

From the project root:

```bash
docker compose up -d --build
```

Services:

- Frontend: http://localhost:8090
- Backend: http://localhost:8081/api
- MySQL: localhost:3306

Check health:

- http://localhost:8081/api/actuator/health

Stop:

```bash
docker compose down
```

## Default Login (Development)

- Username: admin
- Password: admin123

This default user is created by backend seed logic if it does not already exist.

## Useful Commands

Backend compile:

```powershell
.\backend\mvnw.cmd -f .\backend\pom.xml -DskipTests compile
```

Frontend production build:

```bash
npm run build
```

## Notes

- This README is intentionally simple and practical.
- For production, update secrets (especially JWT_SECRET and DB credentials) and restrict allowed origins.
