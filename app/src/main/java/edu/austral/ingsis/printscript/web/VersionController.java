package edu.austral.ingsis.printscript.web;

import java.util.Map;

import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Version del motor PrintScript que trae este adaptador. El handler la consulta y, cuando cambia
 * (se desplego un adaptador con un motor nuevo), revalida todos los snippets de PrintScript. Es
 * publica como /health: no expone nada del usuario y el adaptador solo se alcanza por la red
 * interna.
 */
@RestController
public class VersionController {

    private final String version;

    public VersionController(BuildProperties build) {
        this.version = build.get("printscript");
    }

    @GetMapping("/version")
    public Map<String, String> version() {
        return Map.of("version", version);
    }
}
