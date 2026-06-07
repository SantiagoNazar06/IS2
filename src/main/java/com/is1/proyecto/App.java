package com.is1.proyecto; // Define el paquete de la aplicación, debe coincidir con la estructura de carpetas.

import com.is1.proyecto.config.DarkModeFilter; // Filtro de modo oscuro global.
import com.is1.proyecto.config.DBConfigSingleton; // Clase Singleton para la configuración de la base de datos.
import com.is1.proyecto.config.DBConnectionFilter; // Filtros de conexión a la base de datos.
import com.is1.proyecto.repositories.*;
import com.is1.proyecto.routes.AssignmentRoutes;
import com.is1.proyecto.routes.CareerRoutes;
import com.is1.proyecto.routes.EvaluationRoutes;
import com.is1.proyecto.routes.StudentRoutes;
import com.is1.proyecto.routes.SubjectRoutes;
import com.is1.proyecto.routes.StudyPlanRoutes;
import com.is1.proyecto.routes.TeacherRoutes;
import com.is1.proyecto.routes.UserApiRoutes;
import com.is1.proyecto.routes.UserRoutes;
import com.is1.proyecto.security.AuthService;
import com.is1.proyecto.security.SecurityFilter;
import com.is1.proyecto.services.CareerService;
import com.is1.proyecto.services.EvaluationService;
import com.is1.proyecto.services.ConditionService;
import com.is1.proyecto.services.StudentService;
import com.is1.proyecto.services.SubjectService;
import com.is1.proyecto.services.StudyPlanService;
import com.is1.proyecto.services.TeacherService;
import com.is1.proyecto.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import spark.Spark;
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
        int serverPort = Integer.parseInt(System.getProperty("server.port", "8080"));
        port(serverPort);

        // --- Configuración de archivos estáticos ---
        Spark.staticFiles.location("/public");

        // --- Configuración de la base de datos ---
        DBConfigSingleton dbConfig = DBConfigSingleton.getInstance();

        // Inicializa el schema si la DB esta vacia (ejecuta scheme.sql)
        dbConfig.bootstrap();

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

        // --- Instanciacion de repositorios ---
        CareerRepository careerRepository = new CareerRepository();
        EvaluationRepository evaluationRepository = new EvaluationRepository();
        ConditionRepository conditionRepository = new ConditionRepository();
        PersonRepository personRepository = new PersonRepository();
        StudentRepository studentRepository = new StudentRepository();
        SubjectRepository subjectRepository = new SubjectRepository();
        StudyPlanRepository studyPlanRepository = new StudyPlanRepository();

        // --- Instanciación de servicios ---
        // Los servicios se crean una sola vez y se inyectan en las rutas
        AuthService authService = new AuthService();
        UserService userService = new UserService(authService);
        TeacherRepository teacherRepository = new TeacherRepository();
        TeacherService teacherService = new TeacherService(teacherRepository);
        EnrollmentRepository enrollmentRepository = new EnrollmentRepository();
        StudentService studentService = new StudentService(studentRepository, enrollmentRepository, evaluationRepository);
        CareerService careerService = new CareerService(careerRepository);
        EvaluationService evaluationService = new EvaluationService(evaluationRepository);
        ConditionService conditionService = new ConditionService(conditionRepository);
        SubjectService subjectService = new SubjectService(subjectRepository);
        StudyPlanService studyPlanService = new StudyPlanService(studyPlanRepository);

        // --- Filtro de seguridad ---
        // Debe registrarse ANTES de las rutas para interceptar todas las requests
        // El SecurityFilter valida autenticación y roles
        SecurityFilter.setAuthService(authService);
        SecurityFilter.register();

        // --- Registro de rutas ---
        // Cada grupo de rutas se registra con sus servicios correspondientes
        new UserRoutes(authService, userService, personRepository, teacherService).register();
        new AssignmentRoutes(teacherService).register();
        ObjectMapper objectMapper = new ObjectMapper();
        new TeacherRoutes(teacherService, evaluationService, objectMapper).register();
        new StudentRoutes(studentService, careerService, subjectService, objectMapper).register();
        new CareerRoutes(careerService).register();
        new EvaluationRoutes(evaluationService).register();
        new StudyPlanRoutes(studyPlanService, careerService, subjectService).register();
        new SubjectRoutes(subjectService, conditionService, careerService, studyPlanService).register();
        new UserApiRoutes(authService, userService, objectMapper).register();

        // --- Filtro de modo oscuro ---
        // Debe registrarse al final, después de todas las rutas y filtros,
        // para garantizar que el body HTML ya esté completamente renderizado.
        DarkModeFilter.register();
    }
}
