package com.vigentis.comun;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * La web de React vive dentro del backend (carpeta resources/static).
 * Si alguien abre o recarga directamente una pantalla como /riesgos,
 * se entrega la misma página de React y ella muestra la pantalla correcta.
 */
@Controller
public class WebSpaController {

    @GetMapping({"/activos", "/riesgos", "/criterios", "/controles", "/incidentes", "/auditorias",
                 "/documentos", "/reportes", "/historial", "/usuarios", "/mi-cuenta"})
    public String pantalla() {
        return "forward:/index.html";
    }
}
