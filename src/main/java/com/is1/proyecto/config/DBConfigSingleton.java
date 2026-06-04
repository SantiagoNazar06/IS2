// Archivo: com/is1/proyecto/config/DBConfigSingleton.java
package com.is1.proyecto.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;

import org.javalite.activejdbc.Base;

import com.is1.proyecto.config.DataSeeder;

public final class DBConfigSingleton {

    private static DBConfigSingleton instance;

    private final String user;
    private final String pass;
    private final String driver;

    private DBConfigSingleton() {
        this.driver = "org.sqlite.JDBC";
        this.user = "";
        this.pass = "";
    }

    public static synchronized DBConfigSingleton getInstance() {
        if (instance == null) {
            instance = new DBConfigSingleton();
        }
        return instance;
    }

    /**
     * Inicializa la base de datos: si las tablas no existen, ejecuta el schema
     * desde src/main/resources/scheme.sql; si ya existen, ejecuta migraciones.
     * <p>
     * Debe llamarse una vez al iniciar la aplicacion, ANTES de cualquier
     * operacion contra la DB.
     */
    public void bootstrap() {
        try (Connection conn = DriverManager.getConnection(getDbUrl())) {
            if (tablesExist(conn)) {
                System.out.println("[DB] Schema ya inicializado, ejecutando migraciones...");
                runMigrations(conn);
            } else {
                System.out.println("[DB] Base de datos vacia, ejecutando scheme.sql...");
                runSqlScript(conn, loadSchemaSql());
                System.out.println("[DB] Schema ejecutado correctamente.");
            }

            // Sembrar datos iniciales (corre siempre, es idempotente por username)
            Base.open(this.driver, getDbUrl(), this.user, this.pass);
            try {
                DataSeeder.seed();
            } finally {
                Base.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Fallo al inicializar la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Migra el schema existente para agregar columnas y actualizar constraints.
     */
    private void runMigrations(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Migracion 1: columna teacher_id
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN teacher_id INTEGER REFERENCES teachers(id)");
                System.out.println("[DB] Migracion: columna teacher_id agregada.");
            } catch (Exception e) {
                // Ya existe, ignorar
            }

            // Migracion 2: CHECK constraint con TEACHER
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='users'")) {
                if (rs.next()) {
                    String createSql = rs.getString("sql");
                    if (createSql != null && !createSql.contains("TEACHER")) {
                        System.out.println("[DB] Migracion: actualizando CHECK constraint...");
                        stmt.execute("ALTER TABLE users RENAME TO users_old");
                        stmt.execute("CREATE TABLE users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "name TEXT NOT NULL UNIQUE," +
                            "password TEXT NOT NULL," +
                            "role TEXT NOT NULL DEFAULT 'STUDENT' CHECK(role IN('ADMIN','STUDENT','TEACHER'))," +
                            "student_id INTEGER REFERENCES students(id)," +
                            "teacher_id INTEGER REFERENCES teachers(id)" +
                        ")");
                        stmt.execute("INSERT INTO users (id, name, password, role, student_id, teacher_id) " +
                            "SELECT id, name, password, role, student_id, teacher_id FROM users_old");
                        stmt.execute("DROP TABLE users_old");
                        System.out.println("[DB] Migracion: CHECK constraint actualizado.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DB] Migracion fallo (no critico): " + e.getMessage());
        }
    }

    /**
     * Ejecuta un script SQL multilinea (separado por ;).
     */
    private void runSqlScript(Connection conn, String sql) throws Exception {
        for (String statement : sql.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    /**
     * Verifica si al menos la tabla 'users' existe (asumimos schema completo si existe).
     */
    private boolean tablesExist(Connection conn) {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, "users", null)) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lee el contenido de scheme.sql desde el classpath.
     */
    private String loadSchemaSql() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("scheme.sql");
        if (is == null) {
            throw new IllegalStateException(
                "No se encontro scheme.sql en el classpath. " +
                "Verifica que exista en src/main/resources/scheme.sql");
        }
        return new BufferedReader(new InputStreamReader(is))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    // Métodos para abrir y cerrar la conexión
    public void openConnection() {
        Base.open(this.driver, getDbUrl(), this.user, this.pass);
    }

    public void closeConnection() {
        Base.close();
    }

    public String getDbUrl() {
        return System.getProperty("db.url", "jdbc:sqlite:./db/dev.db");
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public String getDriver() {
        return driver;
    }
}

