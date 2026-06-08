package com.is1.proyecto.config;

import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;

/**
 * Filtro 'afterAfter' que inyecta recursos de modo oscuro en respuestas HTML.
 * <p>
 * Se ejecuta después de todos los filtros 'after' y del renderizado de templates,
 * cuando {@code res.body()} contiene el HTML completo renderizado.
 * <p>
 * Responsabilidades:
 * <ol>
 *   <li>Inyectar el link al CSS de modo oscuro ({@code /css/dark-mode.css}) antes de {@code </head>}</li>
 *   <li>Inyectar el script JS ({@code /js/dark-mode.js}) antes de {@code </head>}</li>
 *   <li>Inyectar el botón flotante de alternancia antes de {@code </body>}</li>
 * </ol>
 * <p>
 * Solo procesa respuestas HTML (según el header {@code Accept} del request).
 * Las rutas JSON (API REST) no se ven afectadas.
 * <p>
 * Importante: usa {@code req.headers("Accept")} en vez de {@code res.type()}
 * porque Spark setea el content-type en {@code Body.serializeTo()}, que corre
 * <b>después</b> de los filtros {@code afterAfter}.
 */
public class DarkModeFilter {

    private static final String CSS_LINK = "<link rel=\"stylesheet\" href=\"/css/dark-mode.css\">";
    private static final String JS_SCRIPT = "<script src=\"/js/dark-mode.js\"></script>";

    private static final String TOGGLE_BUTTON = ""
            + "<button id=\"darkModeToggle\""
            + " aria-label=\"Cambiar modo oscuro\""
            + " style=\"position:fixed;top:1rem;left:1rem;z-index:9999;"
            + "width:2.5rem;height:2.5rem;border-radius:9999px;background:#1f2937;"
            + "color:#f9fafb;border:2px solid #374151;cursor:pointer;font-size:1.25rem;"
            + "display:flex;align-items:center;justify-content:center;"
            + "box-shadow:0 4px 12px rgba(0,0,0,0.25);transition:all 0.3s ease;"
            + "\">\uD83C\uDF19</button>";

    /**
     * Crea el filtro 'afterAfter' que procesa respuestas HTML.
     * <p>
     * Verifica que el request acepte {@code text/html}, lee el cuerpo renderizado,
     * inyecta los assets antes de {@code </head>} y el botón antes de {@code </body>},
     * y reemplaza el body con la versión modificada.
     * <p>
     * Nota: NO usa {@code res.type()} porque Spark setea el content-type después
     * de los filtros, en {@code Body.serializeTo()}.
     *
     * @return el filtro listo para ser registrado
     */
    private static Filter createFilter() {
        return (Request req, Response res) -> {
            try {
                // Solo procesar respuestas HTML
                String accept = req.headers("Accept");
                if (accept == null || !accept.contains("text/html")) {
                    return;
                }

                String body = res.body();
                if (body == null || body.isEmpty()) {
                    return;
                }

                // Inyectar CSS y JS antes de </head>
                String headTag = "</head>";
                int headIndex = body.indexOf(headTag);
                if (headIndex == -1) {
                    return; // Safety check: no </head> tag found
                }

                StringBuilder modified = new StringBuilder(body);
                String headInjection = "\n    " + CSS_LINK + "\n    " + JS_SCRIPT + "\n  ";
                modified.insert(headIndex, headInjection);

                // Inyectar botón flotante antes de </body>
                String bodyTag = "</body>";
                int bodyIndex = modified.indexOf(bodyTag);
                if (bodyIndex == -1) {
                    return; // Safety check: no </body> tag found
                }

                String bodyInjection = "\n  " + TOGGLE_BUTTON + "\n";
                modified.insert(bodyIndex, bodyInjection);

                res.body(modified.toString());

            } catch (Exception e) {
                System.err.println("Error en DarkModeFilter: " + e.getMessage());
            }
        };
    }

    /**
     * Registra el filtro 'afterAfter' en el pipeline de Spark.
     * <p>
     * Debe llamarse después de registrar todas las rutas y filtros 'before'/'after',
     * para garantizar que el body HTML ya esté completamente renderizado.
     */
    public static void register() {
        Spark.afterAfter(createFilter());
        System.out.println("DarkModeFilter registrado correctamente");
    }
}
