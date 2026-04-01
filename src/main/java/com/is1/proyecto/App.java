package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import com.is1.proyecto.config.DBConfigSingleton; // Clase Singleton para la configuración de la base de datos.
import com.is1.proyecto.config.DBConnectionFilter; // Filtros de conexión a la base de datos.
import com.is1.proyecto.routes.StudentRoutes;
import com.is1.proyecto.routes.TeacherRoutes;
import com.is1.proyecto.routes.UserRoutes;
import com.is1.proyecto.security.AuthService;
import com.is1.proyecto.services.StudentService;
import com.is1.proyecto.services.TeacherService;
import com.is1.proyecto.services.UserService;

import static spark.Spark.port;

/**
 * Clase principal de la aplicación Spark.
 * Configura las rutas, filtros y el inicio del servidor web.
 * 
 * Esta clase actúa como bootstrapper: solo instancia los servicios y registra las rutas.
 * Toda la lógica de negocio ha sido extraída a servicios y rutas especializadas.
 */
public class App {

    /**
     * Método principal que se ejecuta al iniciar la aplicación.
     * Aquí se configuran el servidor, filtros y se registran todas las rutas.
     */
    public static void main(String[] args) {
        // --- Configuración del servidor ---
        port(8080); // Configura el puerto en el que la aplicación Spark escuchará las peticiones (por defecto es 4567).

        // --- Configuración de la base de datos ---
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();
        DBConnectionFilter.init(
            dbConfig.getDriver(), 
            dbConfig.getDbUrl(), 
            dbConfig.getUser(), 
            dbConfig.getPass()
        );
        
        // --- Filtros globales ---
        // Filtro 'before' para abrir conexión a la DB
        // Filtro 'after' para cerrar conexión a la DB
        DBConnectionFilter.register();

        // --- Instanciación de servicios ---
        // Los servicios se crean una sola vez y se inyectan en las rutas
        AuthService authService = new AuthService();
        UserService userService = new UserService(authService);
        TeacherService teacherService = new TeacherService();
        StudentService studentService = new StudentService();

        // --- Registro de rutas ---
        // Cada grupo de rutas se registra con sus servicios correspondientes
        new UserRoutes(authService, userService).register();
        new TeacherRoutes(teacherService).register();
        new StudentRoutes(studentService).register();
    }
}
