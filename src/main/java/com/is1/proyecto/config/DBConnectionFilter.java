package com.is1.proyecto.config;

import org.javalite.activejdbc.Base;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Spark;

/**
 * Filtros de conexión a la base de datos.
 * Gestiona la apertura y cierre de conexiones ActiveJDBC para cada solicitud HTTP.
 */
public class DBConnectionFilter {

    private static String driver;
    private static String dbUrl;
    private static String user;
    private static String pass;

    /**
     * Inicializa el filtro con la configuración de la base de datos.
     */
    public static void init(String driver, String dbUrl, String user, String pass) {
        DBConnectionFilter.driver = driver;
        DBConnectionFilter.dbUrl = dbUrl;
        DBConnectionFilter.user = user;
        DBConnectionFilter.pass = pass;
    }

    /**
     * Filtro 'before' que se ejecuta antes de cada solicitud HTTP.
     * Abre una conexión a la base de datos.
     */
    private static Filter createBeforeFilter() {
        return (Request req, Response res) -> {
            try {
                // Abre una conexión a la base de datos utilizando las credenciales configuradas.
                Base.open(driver, dbUrl, user, pass);
                System.out.println(req.url());

            } catch (Exception e) {
                // Si ocurre un error al abrir la conexión, se registra y se detiene la solicitud
                // con un código de estado 500 (Internal Server Error) y un mensaje JSON.
                System.err.println("Error al abrir conexión con ActiveJDBC: " + e.getMessage());
                Spark.halt(500, "{\"error\": \"Error interno del servidor: Fallo al conectar a la base de datos.\"}"
                        + e.getMessage());
            }
        };
    }

    /**
     * Filtro 'after' que se ejecuta después de cada solicitud HTTP.
     * Cierra la conexión a la base de datos para liberar recursos.
     */
    private static Filter createAfterFilter() {
        return (Request req, Response res) -> {
            try {
                // Cierra la conexión a la base de datos para liberar recursos.
                Base.close();
            } catch (Exception e) {
                // Si ocurre un error al cerrar la conexión, se registra.
                System.err.println("Error al cerrar conexión con ActiveJDBC: " + e.getMessage());
            }
        };
    }

    /**
     * Registra los filtros before/after en Spark.
     */
    public static void register() {
        Spark.before(createBeforeFilter());  // Filtro que se ejecuta antes de cada request
        Spark.after(createAfterFilter());    // Filtro que se ejecuta después de cada request
    }
}
