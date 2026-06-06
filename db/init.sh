#!/bin/bash
# =============================================================================
# init.sh — Inicializa la base de datos desde cero
# =============================================================================
# Ejecuta las migraciones en orden y luego los scripts de datos.
#
# Uso:
#   ./db/init.sh                    → crea db/dev.db (por defecto)
#   DB_PATH=./db/prod.db ./db/init.sh → ruta personalizada
#
# Idempotente: se puede ejecutar múltiples veces sin romper nada.
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_PATH="${DB_PATH:-${SCRIPT_DIR}/dev.db}"
MIGRATIONS_DIR="${SCRIPT_DIR}/migrations"

echo "=== Inicializando base de datos ==="
echo "  DB: ${DB_PATH}"

# Eliminar base existente para empezar limpio (opcional)
# Descomentar si se quiere regenerar desde cero:
# rm -f "${DB_PATH}"

# ---------------------------------------------------------------------------
# Migraciones
# ---------------------------------------------------------------------------
echo ""
echo "--- Ejecutando migraciones ---"
for f in "${MIGRATIONS_DIR}"/0*.sql; do
    echo "  → $(basename "${f}")"
    sqlite3 "${DB_PATH}" < "${f}"
done
echo "  ✓ Migraciones completadas."

# ---------------------------------------------------------------------------
# Seed data (datos mínimos para que la app funcione)
# ---------------------------------------------------------------------------
echo ""
echo "--- Sembrando datos iniciales ---"
sqlite3 "${DB_PATH}" < "${SCRIPT_DIR}/seed.sql"
echo "  ✓ Seed completado."

# ---------------------------------------------------------------------------
# Test data (datos de prueba opcionales)
# ---------------------------------------------------------------------------
if [ -z "${SKIP_DATA:-}" ]; then
    echo ""
    echo "--- Cargando datos de prueba ---"
    sqlite3 "${DB_PATH}" < "${SCRIPT_DIR}/data.sql"
    echo "  ✓ Datos de prueba cargados."
fi

# ---------------------------------------------------------------------------
# Resumen
# ---------------------------------------------------------------------------
echo ""
echo "=== Base de datos inicializada ==="
echo "  DB: ${DB_PATH}"
echo ""
echo "Usuarios disponibles (dev):"
echo "  admin   / admin123   (Administrador)"
echo "  teacher / teacher123 (Profesor)"
echo "  student / student123 (Estudiante)"
echo ""
echo "Para saltar datos de prueba: SKIP_DATA=1 ./db/init.sh"
