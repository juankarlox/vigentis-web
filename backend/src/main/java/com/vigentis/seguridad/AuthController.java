package com.vigentis.seguridad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio") String correo,
            @NotBlank(message = "La contraseña es obligatoria") String clave) {}

    public record RegistroRequest(
            @NotBlank(message = "El nombre de la empresa es obligatorio") @Size(max = 150, message = "Máximo 150 caracteres")
            String empresa,
            @Size(max = 30, message = "Máximo 30 caracteres") String nit,
            @Size(max = 60, message = "Máximo 60 caracteres") String sector,
            @NotBlank(message = "Tu nombre es obligatorio") @Size(max = 100, message = "Máximo 100 caracteres")
            String nombre,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo no válido")
            @Size(max = 150, message = "Máximo 150 caracteres") String correo,
            @NotBlank(message = "La contraseña es obligatoria") String clave) {}

    public record CambioClaveRequest(
            @NotBlank(message = "Escribe tu contraseña actual") String actual,
            @NotBlank(message = "Escribe la nueva contraseña") String nueva) {}

    private final AuthService servicio;

    public AuthController(AuthService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    public AuthService.Sesion login(@Valid @RequestBody LoginRequest datos) {
        return servicio.login(datos.correo(), datos.clave());
    }

    // "Registrar mi empresa": crea la empresa y su administrador, y entra directamente
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.Sesion registro(@Valid @RequestBody RegistroRequest d) {
        return servicio.registrar(new AuthService.Registro(d.empresa(), d.nit(), d.sector(), d.nombre(), d.correo(), d.clave()));
    }

    @GetMapping("/yo")
    public AuthService.Perfil yo() {
        return servicio.yo();
    }

    @PutMapping("/clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarClave(@Valid @RequestBody CambioClaveRequest datos) {
        servicio.cambiarClave(datos.actual(), datos.nueva());
    }
}
