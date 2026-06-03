package com.is1.proyecto.templates;

import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que login.mustache no contenga el link de registro.
 */
class LoginTemplateTest {

    @Test
    void loginTemplate_doesNotContainRegisterLink() throws Exception {
        String content = loadTemplate("templates/login.mustache");
        // El link "Regístrate aquí" apuntando a /user/new no debe existir
        assertFalse(content.contains("Regístrate aquí"),
                "login.mustache no debe contener el link 'Regístrate aquí'");
        assertFalse(content.contains("/user/new"),
                "login.mustache no debe referenciar /user/new como link de registro");
    }

    @Test
    void loginTemplate_stillContainsLoginForm() throws Exception {
        String content = loadTemplate("templates/login.mustache");
        assertTrue(content.contains("Iniciar Sesión"),
                "login.mustache debe mantener el formulario de login");
        assertTrue(content.contains("/login"),
                "login.mustache debe mantener la acción del formulario");
        assertTrue(content.contains("Volver al inicio"),
                "login.mustache debe mantener el link 'Volver al inicio'");
    }

    private String loadTemplate(String path) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(is, "Template not found: " + path);
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }
}
