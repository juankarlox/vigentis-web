package com.vigentis.riesgos;

import jakarta.validation.constraints.*;
import java.util.List;

public record CriteriosRequest(
        @NotNull @Min(value = 2, message = "Mínimo 2") @Max(value = 23, message = "Máximo 23")
        Integer umbralMedio,

        @NotNull @Min(value = 3, message = "Mínimo 3") @Max(value = 24, message = "Máximo 24")
        Integer umbralAlto,

        @NotNull @Min(value = 4, message = "Mínimo 4") @Max(value = 25, message = "Máximo 25")
        Integer umbralCritico,

        @NotNull @Min(value = 1, message = "Entre 1 y 25") @Max(value = 25, message = "Entre 1 y 25")
        Integer nivelAceptable,

        @NotNull @Min(value = 1, message = "Entre 1 y 36 meses") @Max(value = 36, message = "Entre 1 y 36 meses")
        Integer mesesRevision,

        @Size(min = 5, max = 5, message = "Deben ser 5 descripciones")
        List<@Size(max = 200, message = "Máximo 200 caracteres") String> escalaProbabilidad,

        @Size(min = 5, max = 5, message = "Deben ser 5 descripciones")
        List<@Size(max = 200, message = "Máximo 200 caracteres") String> escalaImpacto
) {}
