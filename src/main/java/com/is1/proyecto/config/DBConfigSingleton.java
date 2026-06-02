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
     * desde src/main/resources/scheme.sql.
     * <p>
     * Debe llamarse una vez al iniciar la aplicacion, ANTES de cualquier
     * operacion contra la DB.
     */
    public void bootstrap() {
        try (Connection conn = DriverManager.getConnection(getDbUrl())) {
            if (tablesExist(conn)) {
                System.out.println("[DB] Schema ya inicializado, omitiendo bootstrap.");
                return;
            }
            System.out.println("[DB] Base de datos vacia, ejecutando scheme.sql...");
            String sql = loadSchemaSql();
            // SQLite solo ejecuta una sentencia por execute(), partimos por ';'
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            System.out.println("[DB] Schema ejecutado correctamente.");
        } catch (Exception e) {
            throw new RuntimeException("Fallo al inicializar la base de datos: " + e.getMessage(), e);
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

    public void openConnection() {
        if (!Base.hasConnection()) {
            Base.open(this.driver, getDbUrl(), this.user, this.pass);
        }
    }

    public void closeConnection() {
        if (Base.hasConnection()) {
            Base.close();
        }
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

