# QA-Agent Service Startup Guide

Start, check, and stop the local development services using Windows PowerShell.

**Updated:** 14 October 2026

**Project folder:** `C:\Users\oisin\Downloads\qa-agent`

Use **two separate PowerShell terminals**, one for each service. Leave each terminal open while its service is running. Adjust the paths below if your project is stored elsewhere.

## Services at a glance

| Terminal | Service | Port | Browser check |
| --- | --- | --- | --- |
| 1 | Java backend | 8080 | [Health endpoint](http://localhost:8080/api/health) |
| 2 | React frontend | 5173 | [Application](http://localhost:5173) |

Java 21 and Spring Boot provide the only backend. The health endpoint and requirement preview do not require a database, Docker, or an LLM connection. Requirements are validated and previewed only; they are not saved.

## Contents

- [Start the Java backend](#1-start-the-java-backend)
- [Start the React frontend](#2-start-the-react-frontend)
- [Check requirement submission](#check-requirement-submission)
- [Stop and restart services](#stop-and-restart-services)
- [Install missing dependencies](#install-missing-dependencies)
- [Check builds before committing](#check-builds-before-committing)
- [Troubleshooting](#troubleshooting)
- [Documentation](#documentation)

## 1. Start the Java backend

Run in **Terminal 1**:

```powershell
Set-Location "C:\Users\oisin\Downloads\qa-agent\backend"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run
```

Open [http://localhost:8080/api/health](http://localhost:8080/api/health). Expected response:

```json
{"status":"UP"}
```

> Replace `JAVA_HOME` with your JDK 21 installation path if it differs. These environment settings apply only to the current PowerShell session.

To check Java before starting the backend, run these commands from the `backend` folder after setting `JAVA_HOME` and `Path`:

```powershell
java -version
.\mvnw.cmd -version
```

Both should report Java 21 for this project setup.

## 2. Start the React frontend

Run in **Terminal 2**:

```powershell
Set-Location "C:\Users\oisin\Downloads\qa-agent\frontend"
npm.cmd run dev
```

Open the **Local** URL printed by Vite, normally [http://localhost:5173](http://localhost:5173).

- If port 5173 is busy, Vite may print a different port. Use the printed URL.
- The backend status should show **Connected** when Java is running and the frontend health request and development proxy are configured.
- If you started the frontend first, refresh the page after starting Java.

## Check requirement submission

- Confirm the page shows **Backend connection: Connected**.
- Submit a title, description, and acceptance criteria; confirm the accepted preview appears.
- Confirm spaces-only fields are rejected.
- Refresh the page; the preview should disappear because requirements are not saved.
- Stop Java and refresh; confirm the backend unavailable message appears.

## Stop and restart services

1. Select the terminal running the service.
2. Press **Ctrl+C**.
3. If asked to terminate a batch job, enter **Y**.

Repeat for both services when finished.

To restart a service, run its startup command again from its folder. In a new backend terminal, repeat the `JAVA_HOME` and `Path` commands first. Dependencies do not need to be reinstalled every time.

## Install missing dependencies

### Frontend

If `node_modules` is missing:

```powershell
Set-Location "C:\Users\oisin\Downloads\qa-agent\frontend"
npm.cmd install
```

### Java backend

The Maven wrapper downloads its dependencies when needed.

## Check builds before committing

These commands finish and return to the prompt. They do not start the services.

### Backend

Stop the Java backend before running its clean build. Use a terminal configured for JDK 21 as described above.

```powershell
Set-Location "C:\Users\oisin\Downloads\qa-agent\backend"
.\mvnw.cmd clean verify
```

### Frontend

```powershell
Set-Location "C:\Users\oisin\Downloads\qa-agent\frontend"
npm.cmd run build
npm.cmd run lint
```

## Troubleshooting

| Problem | What to check |
| --- | --- |
| npm cannot find `package.json` | Run npm commands from `frontend`, not `backend` or the project root. |
| `node` or `npm.cmd` is not recognised | Check that the runtime is installed, then reopen PowerShell. Check with `node --version` or `npm.cmd --version`. |
| Port or address already in use | Check whether the service is already running in another terminal. Stop that instance with **Ctrl+C** before starting another on the same port. |
| Frontend shows a backend connection error | Open [the Java health endpoint](http://localhost:8080/api/health), check the Java terminal for errors, then refresh the frontend once Java responds. |

## Documentation

- [Project reference list](reference.md)
- [Vite — Getting Started](https://vite.dev/guide/)
- [Spring Boot — Running Your Application](https://docs.spring.io/spring-boot/maven-plugin/run.html)
