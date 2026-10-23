package com.vigentis.auditorias;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record AuditoriaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "Máximo 200 caracteres")
        String nombre,

        @NotNull(message = "El tipo es obligatorio (INTERNA o EXTERNA)")
        Auditoria.Tipo tipo,

        @NotNull(message = "La fecha de la auditoría es obligatoria")
        @PastOrPresent(message = "La fecha de la auditoría no puede estar en el futuro")
        LocalDate fechaAuditoria,

        @Size(max = 150, message = "Máximo 150 caracteres")
        String auditor
) {}
