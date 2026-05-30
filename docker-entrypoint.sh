#!/bin/sh
# Docker entrypoint for IS2 — transforms env vars to JVM -D properties
# Uses exec so Java receives signals directly (no sh as PID 1)
exec java \
  -Ddb.url="${DB_URL}" \
  -Dserver.port="${SERVER_PORT}" \
  -jar /app/proye-is-*.jar
