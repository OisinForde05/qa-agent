# QA-Agent Service Startup Guide

Start, check, and stop the local development services using Windows PowerShell.

**Updated:** 4 October 2026  
**Project folder:** `C:\Users\oisin\Downloads\qa-agent`

Use **three separate PowerShell terminals**, one for each service. Leave each terminal open while its service is running. Adjust the paths below if your project is stored elsewhere.

## Services at a glance

| Terminal | Service | Port | Browser check |
| --- | --- | --- | --- |
| 1 | Python AI service | 8000 | [Health endpoint](http://localhost:8000/health) |
| 2 | Java backend | 8080 | [Health endpoint](http://localhost:8080/api/health) |
| 3 | React frontend | 5173 | [Application](http://localhost:5173) |

The AI service commands require `ai-service/main.py` and its dependencies from the setup step. These health endpoints do not require a database, Docker, or an LLM connection. Connecting Java to Python is a separate development step.

## Contents

- [Start the Python AI service](#1-start-the-python-ai-service)
- [Start the Java backend](#2-start-the-java-backend)
- [Start the React frontend](#3-start-the-react-frontend)
- [Stop and restart services](#stop-and-restart-services)
- [Install missing dependencies](#install-missing-dependencies)
- [Check builds before committing](#check-builds-before-committing)
- [Troubleshooting](#troubleshooting)
- [Documentation](#documentation)

## 1. Start the Python AI service

Run in **Terminal 1**:

```powershell
Set-Location "C:\Users\oisin\Downloads\qa-agent\ai-service"
.\.venv\Scripts\python.exe -m uvicorn main:app --reload --port 8000
```

Open [http://localhost:8000/health](http://localhost:8000/health). Expected response:

```json
{"status":"UP"}
```

Interactive API documentation is available at [http://localhost:8000/docs](http://localhost:8000/docs).

> The health endpoint confirms that the service responds. It does not confirm an LLM connection or test-generation functionality.

## 2. Start the Java backend

Run in **Terminal 2**:

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

## 3. Start the React frontend

Run in **Terminal 3**:

```powershell
Set-Location "C:\Users\oisin\Downloads\qa-agent\frontend"
npm.cmd run dev
```

Open the **Local** URL printed by Vite, normally [http://localhost:5173](http://localhost:5173).

- If port 5173 is busy, Vite may print a different port. Use the printed URL.
- The backend status should show **Connected** when Java is running and the frontend health request and development proxy are configured.
- If you started the frontend first, refresh the page after starting Java.

## Stop and restart services

1. Select the terminal running the service.
2. Press **Ctrl+C**.
3. If asked to terminate a batch job, enter **Y**.

Repeat for all three services when finished.

To restart a service, run its startup command again from its folder. In a new backend terminal, repeat the `JAVA_HOME` and `Path` commands first. Dependencies do not need to be reinstalled every time.

## Install missing dependencies

### Frontend

If `node_modules` is missing:

```powershell
Set-Location "C:\Users\oisin\Downloads\qa-agent\frontend"
npm.cmd install
```

### Python AI service

If `.venv` is missing, create it and install the recorded dependencies:

```powershell
Set-Location "C:\Users\oisin\Downloads\qa-agent\ai-service"
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

For the **initial setup only**, if `requirements.txt` does not yet exist, run the following after creating `.venv`:

```powershell
.\.venv\Scripts\python.exe -m pip install fastapi uvicorn
.\.venv\Scripts\python.exe -m pip freeze | Set-Content -Encoding utf8 requirements.txt
```

Calling `.venv`'s `python.exe` directly means activation is not necessary.

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

### Python AI service

Start the service and check that [the health endpoint](http://localhost:8000/health) returns `{"status":"UP"}`. This is a manual smoke check, not an automated test suite.

## Troubleshooting

| Problem | What to check |
| --- | --- |
| npm cannot find `package.json` | Run npm commands from `frontend`, not `backend` or the project root. |
| `node`, `npm.cmd`, or `py` is not recognised | Check that the runtime is installed, then reopen PowerShell. Check with `node --version`, `npm.cmd --version`, or `py --version`. |
| `.venv\Scripts\python.exe` cannot be found | Check that you are in `ai-service` and have created its virtual environment. |
| `No module named uvicorn` or `No module named fastapi` | Install requirements using the virtual environment's `python.exe`, as shown above. |
| `Could not import module main` | Check that `main.py` is inside `ai-service` and that your terminal is in that folder. |
| Port or address already in use | Check whether the service is already running in another terminal. Stop that instance with **Ctrl+C** before starting another on the same port. |
| Frontend shows a backend connection error | Open [the Java health endpoint](http://localhost:8080/api/health), check the Java terminal for errors, then refresh the frontend once Java responds. |

## Documentation

- [Project reference list](reference.md)
- [Vite — Getting Started](https://vite.dev/guide/)
- [Spring Boot — Running Your Application](https://docs.spring.io/spring-boot/maven-plugin/run.html)
- [FastAPI — Run a Server Manually](https://fastapi.tiangolo.com/deployment/manually/)
- [Python — Virtual Environments](https://docs.python.org/3/library/venv.html)
- [pip — Recording Installed Dependencies](https://pip.pypa.io/en/stable/cli/pip_freeze/)
