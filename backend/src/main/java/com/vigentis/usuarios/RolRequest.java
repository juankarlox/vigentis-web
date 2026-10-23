package com.vigentis.usuarios;

import jakarta.validation.constraints.*;
import java.util.List;

public record RolRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "Máximo 50 caracteres")
        String nombre,

        @Size(max = 255, message = "Máximo 255 caracteres")
        String descripcion,

        @NotNull(message = "Indica los permisos del rol (puede ser una lista vacía)")
        List<String> permisos
) {}
