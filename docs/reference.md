# QA-Agent Development References

This file records technical sources supporting the development of QA-Agent. Each entry explains how the source relates to the project. Add new references as features are developed; research papers for the literature review can be recorded separately.

## Frontend

### 1. Vite — Getting Started

- **Organisation:** Vite
- **URL:** https://vite.dev/guide/
- **Relevant sections:** Scaffolding Your First Vite Project; Command Line Interface.
- **Used for:** Creating the React and TypeScript frontend, understanding Node.js requirements, and running the development server and production build.
- **Accessed:** 3 October 2026.

### 2. React — useState

- **Organisation:** React
- **URL:** https://react.dev/reference/react/useState
- **Relevant sections:** Reference; Adding state to a component.
- **Used for:** Storing the backend connection status in `App.tsx` and updating the displayed status when the health request succeeds or fails.
- **Accessed:** 3 October 2026.

### 3. React — useEffect

- **Organisation:** React
- **URL:** https://react.dev/reference/react/useEffect
- **Relevant sections:** Connecting to an external system; Fetching data with Effects; Troubleshooting.
- **Used for:** Requesting backend health when the component mounts, cleaning up the request, and understanding the extra development setup/cleanup cycle under Strict Mode.
- **Accessed:** 3 October 2026.

### 4. Vite — Server Options

- **Organisation:** Vite
- **URL:** https://vite.dev/config/server-options#server-proxy
- **Relevant section:** `server.proxy`.
- **Used for:** Configuring the frontend development server to forward `/api` requests to the Spring Boot backend at `http://localhost:8080`. This is a development proxy; production routing requires its own configuration.
- **Accessed:** 3 October 2026.

## Backend and Java Setup

### 5. Spring — Building a RESTful Web Service

- **Organisation:** Spring
- **URL:** https://spring.io/guides/gs/rest-service/
- **Relevant sections:** Starting with Spring Initializr; Create a Resource Controller; Run the Service.
- **Used for:** Creating the Spring Boot backend and understanding `@RestController`, `@GetMapping`, and returning JSON from an HTTP endpoint such as `/api/health`.
- **Accessed:** 3 October 2026.

### 6. Spring Boot — System Requirements

- **Organisation:** Spring
- **URL:** https://docs.spring.io/spring-boot/4.1/system-requirements.html
- **Relevant sections:** Java compatibility; Build Tool Support.
- **Used for:** Checking Java and Maven compatibility for the Spring Boot version used by the backend. The project currently targets Java 21 and uses Spring Boot 4.1.1.
- **Accessed:** 3 October 2026.
- **Note:** Documentation URLs may redirect to a newer release. Check the version shown against `backend/pom.xml` when revisiting this source.

### 7. Spring Boot — Testing Spring Boot Applications

- **Organisation:** Spring
- **URL:** https://docs.spring.io/spring-boot/4.1/reference/testing/spring-boot-applications.html
- **Relevant section:** Detecting Test Configuration.
- **Used for:** Understanding how `@SpringBootTest` searches for application configuration and investigating the “Unable to find a @SpringBootConfiguration” test error. This documents configuration discovery; it does not establish the cause of disappearing compiled files in a particular development environment.
- **Accessed:** 3 October 2026.

### 8. Eclipse Adoptium — Windows MSI Installer Packages

- **Organisation:** Eclipse Adoptium
- **URL:** https://adoptium.net/installation/windows/
- **Relevant sections:** Installation options for PATH and `JAVA_HOME`.
- **Used for:** Installing the Temurin JDK on Windows and understanding how PATH and `JAVA_HOME` select the Java installation used by development tools.
- **Accessed:** 3 October 2026.

## Maintaining This File

For each new source, record its organisation or author, title, URL, relevant section, use in QA-Agent, and actual access date. Link sources to the technical decisions they support. Distinguish documented behaviour from project-specific choices and troubleshooting hypotheses.

These are implementation references. A formal literature review should also cite relevant research studies and follow the referencing style required by the module.
