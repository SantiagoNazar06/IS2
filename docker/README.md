# Docker — IS2 Gestor Académico

## Prerequisites

- Docker Engine 20.10+
- Docker Compose v2+

## Build the Image

```bash
# From the project root
docker build -t is2:latest .
```

This runs a multi-stage build:
1. **Build stage** (`maven:3-eclipse-temurin-21`) — compiles source, runs ActiveJDBC instrumentation, packages a fat JAR via the shade plugin.
2. **Runtime stage** (`eclipse-temurin:21-jre-alpine`) — copies only the JAR and creates the `db/` directory.

## Run with Docker Compose (recommended)

```bash
# Start the app
docker compose up

# Run in background
docker compose up -d

# App is now available at http://localhost:8080
```

The `db/` directory is mounted as a volume, so database data persists across restarts.

## Run with Docker Directly

```bash
docker run -p 8080:8080 \
  -e DB_URL=jdbc:sqlite:./db/dev.db \
  -e SERVER_PORT=8080 \
  -v ./db:/app/db \
  is2:latest
```

## Custom Port

Override the server port via the `SERVER_PORT` environment variable:

### Docker Compose

```bash
SERVER_PORT=9090 docker compose up
```

### Docker Directly

```bash
docker run -p 9090:9090 \
  -e DB_URL=jdbc:sqlite:./db/dev.db \
  -e SERVER_PORT=9090 \
  -v ./db:/app/db \
  is2:latest
```

Then visit `http://localhost:9090`.

## Environment Variables

| Variable      | Default                     | Description                           |
|---------------|-----------------------------|---------------------------------------|
| `DB_URL`      | `jdbc:sqlite:./db/dev.db`   | JDBC connection string for SQLite     |
| `SERVER_PORT` | `8080`                      | HTTP port the Spark server listens on |

## Tear Down

```bash
# Stop the container (preserves image)
docker compose down

# Stop AND remove the image
docker compose down --rmi local
```

## Notes

- **Persistent database**: The `db/` directory is mounted as a volume in docker-compose. Your data survives restarts.
- **Single container**: SQLite does not support concurrent writers. Only run one container at a time.
- **First run**: If `db/dev.db` doesn't exist locally, start with an empty database and use the UI to register users.
