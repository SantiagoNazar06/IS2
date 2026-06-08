package com.is1.proyecto.templates;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que cada rol tenga su propio template de dashboard
 * con las secciones que le corresponden.
 */
class DashboardTemplateTest {

    @Test
    void adminDashboard_hasAdminSections() throws Exception {
        String content = loadTemplate("templates/dashboard_admin.mustache");
        assertTrue(content.contains("Panel de Administración"), "Admin debe tener título propio");
        assertTrue(content.contains("Gestionar Profesores"), "Admin debe tener Gestionar Profesores");
        assertTrue(content.contains("Gestionar Estudiantes"), "Admin debe tener Gestionar Estudiantes");
        assertTrue(content.contains("Gestionar Carreras"), "Admin debe tener Gestionar Carreras");
        assertTrue(content.contains("Gestionar Materias"), "Admin debe tener Gestionar Materias");
        assertTrue(content.contains("Gestionar Calificaciones"), "Admin debe tener calificaciones");
        assertTrue(content.contains("Cerrar Sesión"), "Admin debe tener cerrar sesión");
    }

    @Test
    void teacherDashboard_hasTeacherSections() throws Exception {
        String content = loadTemplate("templates/dashboard_teacher.mustache");
        assertTrue(content.contains("Panel del Docente"), "Teacher debe tener título propio");
        assertTrue(content.contains("Tu Perfil"), "Teacher debe tener Tu Perfil");
        assertTrue(content.contains("Listado de Estudiantes"), "Teacher debe tener listado estudiantes");
        assertTrue(content.contains("Gestionar Calificaciones"), "Teacher debe tener calificaciones");
        assertTrue(content.contains("Cerrar Sesión"), "Teacher debe tener cerrar sesión");
    }

    @Test
    void teacherDashboard_excludesAdminSections() throws Exception {
        String content = loadTemplate("templates/dashboard_teacher.mustache");
        assertFalse(content.contains("Registrar Profesor"), "Teacher NO debe tener Registrar Profesor");
        assertFalse(content.contains("Gestionar Profesores"), "Teacher NO debe tener Gestionar Profesores");
        assertFalse(content.contains("Registrar Estudiante"), "Teacher NO debe tener Registrar Estudiante");
        assertFalse(content.contains("Gestionar Carreras"), "Teacher NO debe tener Gestionar Carreras");
        assertFalse(content.contains("Listado de Profesores"), "Teacher NO debe tener listado profesores");
    }

    @Test
    void studentDashboard_hasStudentSections() throws Exception {
        String content = loadTemplate("templates/dashboard_student.mustache");
        assertTrue(content.contains("Panel del Estudiante"), "Student debe tener título propio");
        assertTrue(content.contains("Tu Perfil"), "Student debe tener Tu Perfil");
        assertTrue(content.contains("Estado de Carrera"), "Student debe tener estado de carrera");
        assertTrue(content.contains("Mis Notas"), "Student debe tener ver notas");
        assertTrue(content.contains("Cerrar Sesión"), "Student debe tener cerrar sesión");
    }

    @Test
    void studentDashboard_excludesAdminAndTeacherSections() throws Exception {
        String content = loadTemplate("templates/dashboard_student.mustache");
        assertFalse(content.contains("Registrar Profesor"), "Student NO debe tener Registrar Profesor");
        assertFalse(content.contains("Registrar Estudiante"), "Student NO debe tener Registrar Estudiante");
        assertFalse(content.contains("Gestionar Carreras"), "Student NO debe tener Gestionar Carreras");
        assertFalse(content.contains("Gestionar Estudiantes"), "Student NO debe tener Gestionar Estudiantes");
        assertFalse(content.contains("Gestionar Calificaciones"), "Student NO debe tener gestionar calificaciones");
        assertFalse(content.contains("Listado de Estudiantes"), "Student NO debe tener listado estudiantes");
        assertFalse(content.contains("Listado de Profesores"), "Student NO debe tener listado profesores");
    }

    @Test
    void adminDashboard_excludesProfileLink() throws Exception {
        String content = loadTemplate("templates/dashboard_admin.mustache");
        assertFalse(content.contains("/profile"), "Admin dashboard NO debe tener enlace a /profile");
        assertFalse(content.contains("Tu Perfil"), "Admin dashboard NO debe tener sección Tu Perfil");
    }

    @Test
    void allDashboards_haveLogout() throws Exception {
        assertTrue(loadTemplate("templates/dashboard_admin.mustache").contains("Cerrar Sesión"));
        assertTrue(loadTemplate("templates/dashboard_teacher.mustache").contains("Cerrar Sesión"));
        assertTrue(loadTemplate("templates/dashboard_student.mustache").contains("Cerrar Sesión"));
    }

    private String loadTemplate(String path) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(is, "Template not found: " + path);
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }
}
