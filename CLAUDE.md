# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BenCloudServer is the Java-based REST API backend for the BenMAP web tool (EPA air quality health impact analysis). It pairs with BenCloudApp (Quasar/Vue UI). The server exposes a REST API via SparkJava on port 4567 and handles long-running analysis tasks via an asynchronous task queue.

Current app version: `1.1.0` (see `ApiUtil.appVersion`). Minimum required DB version: `40` (see `ApiUtil.minimumDbVersion`).

## Build & Run Commands

```bash
# Build the fat JAR (uses Gradle Shadow plugin)
gradle shadowJar

# Run tests
gradle test

# Run a single test class
gradle test --tests "gov.epa.bencloud.HIFConfigTest"

# Run the server from command line
java -Xms3G -Xmx3G -XX:+UseG1GC -XX:MaxMetaspaceSize=1G -jar build/libs/BenCloudServer.jar

# Build Docker image
docker image build . -t bc

# Run Docker container (maps SparkJava port 4567 to host 8080)
docker run -dit --publish 8080:4567 bc
```

In Eclipse or VS Code: right-click `BenCloudServer.java` → Run/Debug as Java Application.

## Configuration

The server loads `bencloud-server.properties` first, then optionally overlays `bencloud-local.properties`. The local file is how developers configure their local DB connection and is **not committed**.

Minimum `bencloud-local.properties` for local development:
```properties
postgresql.host=127.0.0.1
postgresql.port=5432
postgresql.database=benmap
postgresql.user=benmap_system
postgresql.password=<password>
max.task.workers=1
file.store.path=<path to local filestore>
```

**The presence of `bencloud-local.properties` is also the runtime signal** that the server is running locally rather than in Kubernetes (`ApplicationUtil.usingLocalProperties()`). This controls whether background tasks run as local threads or as Kubernetes Jobs.

## Architecture

### Two Entry Points

- **`BenCloudServer`** — the API server. Initializes SparkJava, registers all routes, starts the Quartz job scheduler, and validates the DB version on startup.
- **`BenCloudTaskRunner`** — the background task runner. In production/k8s, each task runs as a separate Kubernetes Job that invokes this class. Locally, tasks run as threads within the API server process.

### Request Handling

All routes are registered in `ApiRoutes` (prefix `/api`). Route handlers delegate to Api classes in `gov.epa.bencloud.api`:

| Class | Domain |
|---|---|
| `AirQualityApi` | Air quality surface data |
| `HIFApi` | Health impact functions and results |
| `ValuationApi` | Valuation functions and results |
| `ExposureApi` | Exposure functions and results |
| `PopulationApi` | Population datasets |
| `IncidenceApi` | Incidence/prevalence datasets |
| `GridDefinitionApi` | Spatial grid definitions |
| `TaskApi` / `TaskQueue` / `TaskComplete` | Task lifecycle |
| `FilestoreApi` | File storage |
| `CoreApi` | User info, version, banner |

### Authentication

Auth uses EPA's **WAM (Web Access Management)** system — an upstream proxy that injects HTTP headers. The server reads these via pac4j `HeaderClient`:

- `uid` — user identifier
- `ismemberof` — colon-separated group list (roles: `BenMAP_Users`, `BenMAP_Admins`)
- `displayname`, `mail` — user display attributes

There is no session or token management in the application itself. For local development, the `Add WAM Headers.js` file in `doc/` can be used as a browser extension to inject these headers.

### Task System

Long-running analyses (HIF, Valuation, Exposure, Grid Import, AQ Import, Result Export) are queued in the database (`task_queue` table) and executed asynchronously.

**Local mode**: `TaskWorker` starts each task as a Java thread running the corresponding `*TaskRunnable` class.

**Production (k8s)**: `TaskWorker` calls `KubernetesUtil.runTaskAsJob()`, which spawns a Kubernetes Job that runs `BenCloudTaskRunner` with `TASK_UUID` and `TASK_RUNNER_UUID` env vars. A HIF task can automatically chain into a Valuation task on completion.

Background Quartz jobs (`jobs/` package) handle: polling the queue for new tasks (`ReadFromQueueJob`) and detecting unresponsive task workers (`CheckForUnresponsiveWorkersJob`).

### Database

PostgreSQL with PostGIS. Connection pool via HikariCP. Database access uses **jOOQ** for type-safe SQL — generated code lives in `src/main/java/gov/epa/bencloud/server/database/jooq/`.

**Regenerating jOOQ classes** (after schema changes): edit `jooq_create/library.xml` with your local DB credentials and run `jooq_create/create.sh`.

**Schema migrations**: sequential SQL patch files in `db/patch-NNN.sql`. Apply patches in order after restoring a base dump. The DB version is tracked in the database itself and checked at startup.

## Branching Strategy

- Branch from `develop` using naming convention `develop-<username>` (personal) or `develop-BWD-NNN` (feature/ticket).
- Merge into `develop` via pull/merge request for review.
- `develop` auto-deploys to the EPA dev environment.
- Release tags (semantic versioning, e.g., `1.1.0`) deploy to stage/production.
- Never commit directly to `develop` or `main`.

## Database Setup (First Time)

See `doc/DEVELOPER_SETUP.md` for full steps. Requires PostgreSQL 11+ with PostGIS. Create `benmap_system` role, restore a database dump, then apply patches sequentially:

```bash
psql -d benmap -h localhost -p 5432 -U benmap_system -W -f db/patch-NNN.sql
```
