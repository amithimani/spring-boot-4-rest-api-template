# Spring Boot 4 RESTful API Template

A foundational template for building RESTful APIs with **Java 25**, **Spring Boot 4**, Spring Data JPA, and OpenAPI (Swagger) documentation.

This project is an upgraded fork of the [DIO Spring Boot 3 RESTful API Template](https://github.com/digitalinnovationone/spring-boot-3-rest-api-template), modernized from Java 17 / Spring Boot 3.1 to Java 25 / Spring Boot 4.

<p align="center">
    <img src="https://img.shields.io/badge/Java-v25-blue.svg" alt="Java" />
    <img src="https://img.shields.io/badge/Spring%20Boot-v4.2.0-brightgreen.svg" alt="Spring Boot" />
    <img src="https://img.shields.io/badge/Gradle-v9.9.2-lightgreen.svg" alt="Gradle" />
    <img src="https://img.shields.io/badge/springdoc--openapi-v3.2.1-85ea2d.svg" alt="springdoc-openapi" />
    <img src="https://img.shields.io/badge/PostgreSQL-supported-blue.svg" alt="PostgreSQL" />
</p>

## Table of Contents

- [Key Features](#key-features)
- [Project Structure](#project-structure)
- [Setup](#setup)
- [Project Details](#project-details)
- [API Usage (Swagger UI Documentation)](#api-usage-swagger-ui-documentation)
- [Docker](#docker)
- [Hosting on Railway](#hosting-on-railway)
- [Contribution](#contribution)
- [License](#license)
- [Authors](#authors)

## Key Features:

- **Java 25 (LTS)**: records for DTOs and virtual threads for request handling.
- **Spring Boot 4 / Spring Framework 7**: modular auto-configuration, Jackson 3, Hibernate 7, Jakarta EE 11.
- **Spring Data JPA**: simple data access with optimistic locking (`@Version`).
- **Bean Validation**: request payloads are validated before reaching the business layer.
- **Problem Details (RFC 9457)**: every error is returned as `application/problem+json`.
- **Resilient HTTP integration**: users fetched from [reqres.in](https://reqres.in) with `RestClient`, a Resilience4j circuit breaker, retries with backoff, timeouts, and structured logging.
- **OpenAPI (Swagger)**: interactive API documentation via springdoc-openapi 3.

## Project Structure:

The project is organized into several packages, each serving a specific purpose:

- `client`: clients for external APIs (`client.reqres` wraps the reqres.in users API).

- `config`: cross-cutting configuration, such as logging of Resilience4j events.

- `controller`: REST controllers exposing the endpoints. The `controller.dto` package holds the request/response records, so the JPA entity is never exposed directly and clients cannot set server-managed fields (ID, XP, version).

- `exception`: custom exceptions and a global exception handler that turns them (and Spring MVC's own errors) into Problem Details responses.

- `model`: JPA (Jakarta Persistence) entities.

- `repository`: data access through Spring Data JPA interfaces.

- `service`: business logic, validation of business rules, and transaction boundaries.

- `Application.java`: the main class that bootstraps the application.

Please note that this structure is a simple suggestion for educational purposes. Each developer is free to adapt and modify this structure according to their project needs and standards.

## Setup

Requirements: JDK 25 (if it is not installed, Gradle downloads it automatically through the toolchain resolver; Gradle itself needs JDK 17+ to start).

1. Clone the repository: `git clone https://github.com/amithimani/spring-boot-4-rest-api-template.git`
2. Start the application (Unix): `./gradlew bootRun`
3. Start the application (Windows): `gradlew.bat bootRun`
4. Run the tests: `./gradlew test`

The `dev` profile (in-memory H2 database and H2 console at `/h2-console`) is used by default. Set `SPRING_PROFILES_ACTIVE=prd` to use PostgreSQL.

## Project Details:

The project focuses on a Heroes API as an example, which includes basic CRUD operations and an experience (XP) system:

| Method | Endpoint | Description |
| ------ | -------- | ----------- |
| GET | `/heroes` | List heroes, sorted by XP (highest first) |
| GET | `/heroes/{id}` | Get one hero (`404` if missing) |
| POST | `/heroes` | Create a hero; only `name` is accepted, XP starts at 0 (`201` + `Location`) |
| PUT | `/heroes/{id}` | Rename a hero; send the last known `version` to get `409` on concurrent changes |
| DELETE | `/heroes/{id}` | Delete a hero (`204`) |
| PATCH | `/heroes/{id}/up` | Vote for a hero: +2 XP (Double XP Event \\o/) |
| GET | `/users` | Users from the first 2 pages of reqres.in (see below) |

### External users (reqres.in)

`GET /users` returns the users from the **first 2 pages** of [`https://reqres.in/api/users`](https://reqres.in/api/users), merged in page order. If reqres.in reports fewer pages, it stops early.

| Concern | Behaviour (configurable in `application.yml`) |
| ------- | --------------------------------------------- |
| Timeouts | connect `2s`, read `3s` (`reqres.connect-timeout`, `reqres.read-timeout`) |
| Retry | up to 3 attempts with exponential backoff (500 ms, 1 s), **only** for I/O errors/timeouts, `5xx` and `429` |
| Circuit breaker | opens when at least 50% of the last 10 calls fail (minimum 5 calls); stays open for 30 s, then lets 2 trial calls through |
| Errors | `503` + Problem Details when reqres.in is down, timing out or rate limiting, or the circuit is open (open circuit / `429` include `Retry-After`); `502` when reqres.in rejects the request (other `4xx`) or returns an invalid body. A failure on any page fails the whole request (no partial results). |
| Logging | every outbound call (method, URL, status, duration; never headers or bodies) at `DEBUG` on `me.dio.client`; retries, gave-up events and circuit state changes on `me.dio.resilience`; a summary per request on `me.dio.service` |

reqres.in works without a key on a shared demo quota with a low daily limit. To use your own free key, set `REQRES_API_KEY`; it is sent as the `x-api-key` header.

Exception handling is globally managed with a `@RestControllerAdvice` to ensure consistent Problem Details responses throughout the entire application.

The H2 in-memory database is used for development and tests; PostgreSQL is used in production.

## API Usage (Swagger UI Documentation)

The API documentation can be found on Swagger UI. To view it, please visit: [Swagger UI](http://localhost:8080/swagger-ui.html).

## Docker

The `Dockerfile` builds the application with JDK 25 and runs it on a JRE 25 image as a non-root user:

```bash
docker build -t heroes-api .
docker run -p 8080:8080 heroes-api
```

The server listens on the `PORT` environment variable when set (default `8080`).

## Hosting on Railway

The application can be hosted on [Railway.app](https://railway.app/) together with a PostgreSQL database. Railway builds the `Dockerfile` automatically, which pins Java 25. Configure these variables on the service:

- `SPRING_PROFILES_ACTIVE=prd`
- `RAILWAY_DB_HOST`, `RAILWAY_DB_PORT`, `RAILWAY_DB_NAME`, `RAILWAY_DB_USERNAME`, `RAILWAY_DB_PASSWORD`
- `REQRES_API_KEY` (optional, for `GET /users`)

The `prd` profile does not create tables automatically; create the `heroes` table beforehand (or add a migration tool such as Flyway).

## Contribution

We welcome contributions! If you encounter any issues or have suggestions for improvements, don't hesitate to open an issue or submit a pull request.

## License

This project is licensed under the MIT License. Please refer to the <a href="LICENSE.md">(LICENSE)</a> file for details.

## Authors

Original template by the DIO team:

<table>
  <tr>
    <td align="center"><a href="https://github.com/falvojr"><img src="https://avatars.githubusercontent.com/u/730492?v=4&s=100" width="100px;" alt=""/><br/><strong>Venilton FalvoJr</strong></a><br/><a href="https://www.linkedin.com/in/falvojr/">LinkedIn</a></td>
    <td align="center"><a href="https://github.com/cami-la"><img src="https://avatars.githubusercontent.com/u/64323124?v=4&s=100" width="100px;" alt=""/><br/><strong>Camila Cavalcante</strong></a><br/><a href="https://www.linkedin.com/in/cami-la/">LinkedIn</a></td>
    <td align="center"><a href="https://github.com/rafaskb"><img src="https://avatars.githubusercontent.com/u/2331058?v=4&s=100" width="100px;" alt=""/><br/><strong>Rafa Skoberg</strong></a><br/><a href="https://www.linkedin.com/in/rafaskoberg/">LinkedIn</a></td>
  </tr>
</table>
