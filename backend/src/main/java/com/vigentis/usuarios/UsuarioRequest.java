package com.vigentis.usuarios;

import jakarta.validation.constraints.*;

public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "Máximo 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Correo no válido")
        @Size(max = 150, message = "Máximo 150 caracteres")
        String correo,

        @NotNull(message = "El rol es obligatorio")
        Long rolId,

        // Obligatoria al crear; al editar, solo si se quiere cambiar
        String clave,

        Boolean activo
) {}
