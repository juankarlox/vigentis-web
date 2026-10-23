package com.vigentis.comun;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("error", "Datos inválidos", "campos", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> formatoInvalido(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Formato de datos no válido (revisa tipos, estados, fechas y números)");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Valor no válido para el parámetro '" + ex.getName() + "'");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<Map<String, Object>> faltaParametro(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "Falta un dato obligatorio: " + ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> archivoGrande(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido (20 MB)");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> reglaBaseDatos(DataIntegrityViolationException ex) {
        return error(HttpStatus.CONFLICT,
                "La operación no cumple una regla de la base de datos (por ejemplo, un responsable que no existe)");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> conEstado(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() == null ? "Error" : ex.getReason()));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }
}
