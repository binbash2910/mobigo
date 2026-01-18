# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mobigo is a carpooling (covoiturage) application built with JHipster 8.11.0. It's a monolithic application with:

- **Backend**: Spring Boot 3.4.5 with Java 17
- **Frontend**: Angular 19 with TypeScript
- **Database**: PostgreSQL with Elasticsearch for search
- **Authentication**: JWT-based security

## Common Commands

### Development

```bash
# Start backend (Spring Boot)
./mvnw

# Start frontend (Angular with hot reload)
./npmw start

# Run both simultaneously
./npmw run watch
```

### Testing

```bash
# Run all backend tests (unit + integration)
./mvnw verify

# Run frontend tests
./npmw test

# Run a single backend test class
./mvnw test -Dtest=RideResourceIT

# Run a specific test method
./mvnw test -Dtest=RideResourceIT#createRide
```

### Building

```bash
# Build production JAR
./mvnw -Pprod clean verify

# Build development JAR
./mvnw -Pdev clean verify
```

### API Code Generation

```bash
# Generate API code from OpenAPI spec (src/main/resources/swagger/api.yml)
./mvnw generate-sources
```

### Docker Services

```bash
# Start all services (PostgreSQL, Elasticsearch, Mailpit)
./npmw run services:up

# Or start individually:
./npmw run docker:db:up           # PostgreSQL
./npmw run docker:elasticsearch:up # Elasticsearch
./npmw run docker:mailpit:up       # Mailpit (fake SMTP)
```

Note: When running `./mvnw`, Spring Boot Docker Compose automatically starts all services defined in `src/main/docker/services.yml`.

**Mailpit** (fake SMTP server for development):

- Web UI: http://localhost:8025 (view sent emails)
- SMTP: localhost:1025

### React Client Generation

```bash
# Generate TypeScript/Axios client from running backend API
./npmw run generate:react-client
```

Requires backend to be running. Generated files: `src/main/react-client/generated/`

### Code Quality

```bash
# Lint frontend
./npmw run lint

# Format code
./npmw run prettier:format
```

## Architecture

### Backend Package Structure (`com.binbash.mobigo`)

```
domain/           # JPA entities with Elasticsearch annotations
repository/       # Spring Data JPA repositories
  search/         # Elasticsearch repositories
service/          # Business logic services
  dto/            # Data Transfer Objects
  mapper/         # MapStruct mappers (Entity <-> DTO)
web/rest/         # REST controllers
config/           # Spring configuration classes
security/         # JWT authentication, SecurityUtils
helper/           # Helper/utility classes
```

### Domain Model (Carpooling Business)

Core entities and their relationships:

- **People**: User profiles (linked to User), can be driver (conducteur) or passenger (passager)
- **Vehicle**: Belongs to People (proprietaire), used for rides
- **Ride**: Trip from departure to arrival city with steps, linked to Vehicle
- **Step**: Intermediate stops in a ride
- **Booking**: Passenger booking for a ride, has Payment
- **Payment**: Payment record with status and method
- **Rating**: Reviews between passengers and drivers
- **Message**: Communication between People
- **Group/GroupMember/GroupAuthority**: Group-based permissions system

### API-First Development

The project uses OpenAPI Generator with delegate pattern:

1. Define API in `src/main/resources/swagger/api.yml`
2. Run `./mvnw generate-sources` to generate interfaces
3. Implement delegate classes with `@Service` annotation

Generated code packages:

- `com.binbash.mobigo.web.api` - API interfaces
- `com.binbash.mobigo.service.api.dto` - API DTOs

### Frontend Structure (`src/main/webapp/app`) - Angular

```
account/          # User account management
admin/            # Admin features (user management, metrics, logs)
core/             # Core services (auth, interceptors)
entities/         # Entity CRUD components (generated)
shared/           # Shared components, directives, pipes
```

### React Client (`src/main/react-client/generated/`)

Generated TypeScript/Axios client for use in separate React applications:

- `api.ts` - All API classes and TypeScript interfaces
- `configuration.ts` - API configuration (basePath, accessToken)

Usage:

```typescript
import { Configuration, RideResourceApi } from './generated';

const config = new Configuration({
  basePath: 'http://localhost:8080',
  accessToken: 'jwt-token',
});
const rideApi = new RideResourceApi(config);
const rides = await rideApi.getAllRides();
```

### Testing Patterns

- **Integration tests**: Use `@IntegrationTest` annotation (includes `@EmbeddedElasticsearch` and `@EmbeddedSQL`)
- **Cucumber tests**: Located in `src/test/java/com/binbash/mobigo/cucumber/`
- **Unit tests**: Standard JUnit 5 with `*Test.java` naming
- **Frontend tests**: Jest with `*.spec.ts` files co-located with components

### Configuration Files

- `.yo-rc.json` - JHipster configuration
- `.jhipster/*.json` - Entity definitions for JHipster regeneration
- `src/main/resources/config/application-*.yml` - Spring profiles (dev, prod, tls)

## Key Technical Details

- **No Liquibase**: Database migrations have been removed
- **No caching**: Hibernate cache is disabled
- **WebSocket support**: Spring WebSocket for real-time features (tracker)
- **i18n**: French (fr) as native language, English (en) supported
- **MapStruct**: Used for entity-DTO mapping
- **Testcontainers**: Used for PostgreSQL and Elasticsearch in tests
