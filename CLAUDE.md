# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mobigo is a carpooling (covoiturage) application built with JHipster 8.11.0. It's a monolithic application with:

- **Backend**: Spring Boot 3.4.5 with Java 17
- **Frontend**: Angular 19 with TypeScript
- **Database**: PostgreSQL with Elasticsearch for search (ES optional in prod)
- **Authentication**: JWT-based security
- **Deployment**: Render (free tier), Docker-based

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

**Mailpit** (fake SMTP server for development only):

- Web UI: http://localhost:8025 (view sent emails)
- SMTP: localhost:1025
- Only used in dev profile. Production uses Brevo SMTP (see Email section).

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
  search/         # Elasticsearch repositories (no-op proxies in prod)
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
- **Booking**: Passenger booking for a ride, has Payment. Statuses: `EN_ATTENTE`, `CONFIRME`, `REFUSE`, `ANNULE`, `EFFECTUE`
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

## Key Features

### Email System (MailService)

**Dev**: Mailpit (localhost:1025) — emails visible at http://localhost:8025
**Prod**: Brevo SMTP (`smtp-relay.brevo.com:587`, STARTTLS)

Email types:

- **Activation** (`mail/activationEmail`) — account activation link
- **Creation** (`mail/creationEmail`) — new account notification
- **Password Reset** (`mail/passwordResetEmail`) — reset link
- **Booking Notification** (`mail/bookingNotificationEmail`) — rich HTML for booking events

Booking notification actions: `NEW_BOOKING`, `ACCEPTED`, `REJECTED`, `CANCELLED`, `RIDE_CANCELLED`, `COMPLETED`. Sent asynchronously (`@Async`) to both driver and passenger as appropriate.

Templates: `src/main/resources/templates/mail/`
i18n keys: `src/main/resources/i18n/messages*.properties` (prefix `email.booking.*`)

### Identity Verification / OCR (CniVerificationService)

Verifies national ID cards (CNI), passports, and residence permits for Cameroon and France using Tesseract OCR.

**Pipeline:**

1. Upload recto/verso images via `FileStorageService` (stored in `${UPLOAD_DIR}/cni/`)
2. Multi-strategy OCR (6 progressive strategies): bottom crops, Otsu binarization, contrast stretching, sharpening, restricted MRZ charset
3. MRZ parsing via `CniMrzParserService` — supports TD1 (3x30), TD2 (2x36), TD3 (2x44) ICAO formats
4. Fallback: visual text extraction for dates if MRZ fails
5. Compare extracted data with user profile, update verification status (`VERIFIED`, `REJECTED`, `EXPIRED`)

**Tesseract config**: `application.tesseract.data-path` / `application.tesseract.language` (default: `fra+eng`)
**Dockerfile**: installs `tesseract-ocr` + `tesseract-ocr-fra` in runtime image

### Statistics Dashboard (StatisticsService)

Endpoint: `GET /api/statistics/dashboard`

Provides per-user analytics:

- **Driver stats**: total trips, revenue (FCFA), passengers transported, average rating, completion rate
- **Passenger stats**: total bookings, amount spent, trips completed, ratings given
- **Monthly activity**: last 12 months time-series (rides, bookings, revenue, spending)
- **Top routes**: top 5 most frequent routes (as driver + passenger combined)
- **Global summary**: total earnings, spendings, net balance, member since date

### File Storage (FileStorageService)

Stores uploaded files to filesystem with 3 subdirectories:

- `people/` — profile photos (`/api/images/people/people_{id}.jpg`)
- `vehicles/` — vehicle photos (`/api/images/vehicles/vehicle_{id}.jpg`)
- `cni/` — identity document images (absolute paths for OCR, not served via URL)

Base directory: `${UPLOAD_DIR:/var/data/images}`

### CAPTCHA (CaptchaService)

Cloudflare Turnstile verification for registration/login protection.
Secret key via `${turnstile.secret-key}` (optional — skips verification if not configured).

### Elasticsearch (Optional)

Elasticsearch is fully optional in production via proxy-based no-op beans:

- `NoOpSearchRepositoryConfig` (active when `application.elasticsearch.enabled=false`): creates JDK dynamic proxies returning empty results for all 13 search repositories
- `ElasticsearchRepositoryConfig` (active when `application.elasticsearch.enabled=true`): enables real ES repositories
- Production disables ES autoconfiguration and health checks

## Deployment (Render)

**URL**: https://mobigo.onrender.com (free tier, Oregon)

### Dockerfile

Multi-stage build:

1. **Build stage**: Maven 3.9.6 + Eclipse Temurin 21, builds with `-Pprod`
2. **Runtime stage**: Eclipse Temurin 21 JRE + Tesseract OCR (`tesseract-ocr`, `tesseract-ocr-fra`)

JVM settings: `-Xmx300m -Xms128m -XX:MaxMetaspaceSize=128m` (for 512MB container)

### Key Environment Variables

| Variable                        | Description                          | Default                                |
| ------------------------------- | ------------------------------------ | -------------------------------------- |
| `SPRING_DATASOURCE_URL`         | PostgreSQL JDBC URL                  | —                                      |
| `SPRING_DATASOURCE_USERNAME`    | DB username                          | —                                      |
| `SPRING_DATASOURCE_PASSWORD`    | DB password                          | —                                      |
| `SPRING_MAIL_USERNAME`          | Brevo SMTP login                     | —                                      |
| `SPRING_MAIL_PASSWORD`          | Brevo SMTP password                  | —                                      |
| `JHIPSTER_MAIL_FROM`            | Sender email address                 | `noreply@mobigo.onrender.com`          |
| `FRONTEND_URL`                  | React frontend URL (for email links) | `https://rideshare-web-ten.vercel.app` |
| `UPLOAD_DIR`                    | File storage base directory          | `/var/data/images`                     |
| `TESSDATA_PREFIX`               | Tesseract data path                  | `/usr/share/tesseract-ocr/5/tessdata`  |
| `JHIPSTER_CORS_ALLOWED_ORIGINS` | CORS allowed origins                 | —                                      |
| `turnstile.secret-key`          | Cloudflare Turnstile secret          | — (skips if absent)                    |

### Health & Monitoring

- Health check: `GET /management/health`
- Log file: `GET /management/logfile` (JWT admin required), writes to `/app/logs/mobigo.log`
- Prometheus: `GET /management/prometheus`

## Key Technical Details

- **No Liquibase**: Database migrations have been removed; Hibernate `ddl-auto: update` in prod
- **No caching**: Hibernate cache is disabled
- **WebSocket support**: Spring WebSocket for real-time features (tracker)
- **i18n**: French (fr) as native language, English (en) supported
- **MapStruct**: Used for entity-DTO mapping
- **Testcontainers**: Used for PostgreSQL and Elasticsearch in tests
- **Multipart limits**: Max file 10MB, max request 25MB
- **JWT validity**: 24h (86400s), remember-me 30 days
- **Docker Compose**: Disabled in prod (`spring.docker.compose.enabled: false`)
- **Server errors**: 500+ status codes logged at ERROR level with stack trace (`ExceptionTranslator`)
- **Activation flow**: Email link → `GET /api/activate?key=xxx` → backend activates → HTTP 302 redirect to `${frontendUrl}/login?activated=true`
