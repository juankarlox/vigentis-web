package com.vigentis.auditorias;

import com.vigentis.comun.EmpresaActual;
import com.vigentis.comun.Csv;
import com.vigentis.comun.Textos;
import com.vigentis.comun.UsuarioActual;
import com.vigentis.controles.Control;
import com.vigentis.controles.ControlRepository;
import com.vigentis.historial.Historial;
import com.vigentis.historial.HistorialService;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditoriaService {

    public record ResultadoImportacion(Long auditoriaId, String codigo, int hallazgosImportados) {}

    /** Columnas que entiende la importación (la primera fila del CSV). */
    public static final List<String> COLUMNAS = List.of("codigo", "tipo", "descripcion", "control", "fecha_limite");

    private static final Map<String, Hallazgo.Tipo> TIPOS = Map.of(
            "NC_MAYOR", Hallazgo.Tipo.NC_MAYOR,
            "NO_CONFORMIDAD_MAYOR", Hallazgo.Tipo.NC_MAYOR,
            "NC_MENOR", Hallazgo.Tipo.NC_MENOR,
            "NO_CONFORMIDAD_MENOR", Hallazgo.Tipo.NC_MENOR,
            "OBSERVACION", Hallazgo.Tipo.OBSERVACION,
            "OPORTUNIDAD_MEJORA", Hallazgo.Tipo.OPORTUNIDAD_MEJORA,
            "OPORTUNIDAD_DE_MEJORA", Hallazgo.Tipo.OPORTUNIDAD_MEJORA,
            "OM", Hallazgo.Tipo.OPORTUNIDAD_MEJORA);

    private static final DateTimeFormatter FECHA_LATINA = DateTimeFormatter.ofPattern("d/M/yyyy");

    private final AuditoriaRepository repo;
    private final HallazgoRepository hallazgos;
    private final ControlRepository controles;
    private final HistorialService historial;

    public AuditoriaService(AuditoriaRepository repo, HallazgoRepository hallazgos,
                            ControlRepository controles, HistorialService historial) {
        this.repo = repo;
        this.hallazgos = hallazgos;
        this.controles = controles;
        this.historial = historial;
    }

    // ================= AUDITORÍAS =================

    @Transactional(readOnly = true)
    public List<Auditoria> buscar(String texto, Auditoria.Tipo tipo) {
        return repo.buscar(Textos.limpio(texto), tipo);
    }

    @Transactional(readOnly = true)
    public Auditoria obtener(Long id) {
        return repo.findById(id).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la auditoría con id " + id));
    }

    @Transactional
    public Auditoria crear(AuditoriaRequest d) {
        Auditoria a = nuevaAuditoria(d.nombre(), d.tipo(), d.fechaAuditoria(), d.auditor(), null);
        historial.registrar("AUDITORIA", a.getId(), a.getCodigo(), Historial.Accion.CREAR,
                "Auditoría registrada: " + a.getNombre());
        return a;
    }

    @Transactional
    public Auditoria actualizar(Long id, AuditoriaRequest d) {
        Auditoria a = obtener(id);
        a.setNombre(d.nombre().trim());
        a.setTipo(d.tipo());
        a.setFechaAuditoria(d.fechaAuditoria());
        a.setAuditor(Textos.limpio(d.auditor()));
        repo.saveAndFlush(a);
        historial.registrar("AUDITORIA", a.getId(), a.getCodigo(), Historial.Accion.EDITAR,
                "Auditoría actualizada: " + a.getNombre());
        return a;
    }

    /** Borra la auditoría y sus hallazgos (por ejemplo, si se importó el archivo equivocado). */
    @Transactional
    public void eliminar(Long id) {
        Auditoria a = obtener(id);
        long total = hallazgos.countByAuditoriaId(id);
        repo.delete(a);
        historial.registrar("AUDITORIA", a.getId(), a.getCodigo(), Historial.Accion.ELIMINAR,
                "Auditoría eliminada: " + a.getNombre() + " (con " + total + " hallazgos)");
    }

    // ================= IMPORTAR DESDE CSV =================

    @Transactional
    public ResultadoImportacion importar(MultipartFile archivo, String nombre, Auditoria.Tipo tipo,
                                         LocalDate fecha, String auditor) {
        if (archivo == null || archivo.isEmpty()) {
            throw error("Adjunta el archivo con los hallazgos");
        }
        String nombreArchivo = archivo.getOriginalFilename() == null ? "archivo.csv" : archivo.getOriginalFilename();
        if (!nombreArchivo.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw error("Solo se aceptan archivos .csv (en Excel: Archivo > Guardar como > CSV UTF-8)");
        }
        if (Textos.limpio(nombre) == null) throw error("El nombre de la auditoría es obligatorio");
        if (tipo == null) throw error("El tipo de auditoría es obligatorio (INTERNA o EXTERNA)");
        if (fecha == null) throw error("La fecha de la auditoría es obligatoria");
        if (fecha.isAfter(LocalDate.now())) throw error("La fecha de la auditoría no puede estar en el futuro");

        List<Hallazgo> leidos = leerHallazgos(archivo);

        Auditoria a = nuevaAuditoria(nombre, tipo, fecha, auditor, Textos.recortar(nombreArchivo, 255));
        for (Hallazgo h : leidos) {
            h.setAuditoriaId(a.getId());
            hallazgos.save(h);
        }
        hallazgos.flush();
        historial.registrar("AUDITORIA", a.getId(), a.getCodigo(), Historial.Accion.IMPORTAR,
                "Importados " + leidos.size() + " hallazgos desde " + nombreArchivo);
        return new ResultadoImportacion(a.getId(), a.getCodigo(), leidos.size());
    }

    /** Lee y valida todo el archivo. Si alguna fila tiene error, no se importa nada. */
    private List<Hallazgo> leerHallazgos(MultipartFile archivo) {
        List<List<String>> filas;
        try {
            filas = Csv.leer(decodificar(archivo.getBytes()));
        } catch (IOException e) {
            throw error("No se pudo leer el archivo");
        }
        if (filas.size() < 2) throw error("El archivo no tiene hallazgos (solo encabezado o vacío)");

        // Ubicar las columnas por su nombre
        List<String> encabezado = filas.get(0).stream().map(Textos::clave).toList();
        int colCodigo = encabezado.indexOf("codigo");
        int colTipo = encabezado.indexOf("tipo");
        int colDescripcion = encabezado.indexOf("descripcion");
        int colControl = encabezado.indexOf("control");
        int colFecha = encabezado.indexOf("fecha_limite");
        if (colTipo < 0 || colDescripcion < 0) {
            throw error("La primera fila debe tener al menos las columnas 'tipo' y 'descripcion'. "
                    + "Columnas reconocidas: " + String.join(", ", COLUMNAS));
        }

        List<String> errores = new ArrayList<>();
        List<Hallazgo> resultado = new ArrayList<>();
        Set<String> codigos = new HashSet<>();

        for (int i = 1; i < filas.size(); i++) {
            List<String> f = filas.get(i);
            int numeroFila = i + 1;
            Hallazgo h = new Hallazgo();

            String codigo = Textos.limpio(celda(f, colCodigo));
            if (codigo == null) codigo = String.format("H-%02d", resultado.size() + 1);
            if (codigo.length() > 20) errores.add("Fila " + numeroFila + ": el código supera 20 caracteres");
            if (!codigos.add(codigo.toUpperCase(Locale.ROOT))) {
                errores.add("Fila " + numeroFila + ": el código " + codigo + " está repetido");
            }
            h.setCodigo(codigo);

            Hallazgo.Tipo t = TIPOS.get(Textos.clave(celda(f, colTipo)).toUpperCase(Locale.ROOT));
            if (t == null) {
                errores.add("Fila " + numeroFila + ": tipo '" + celda(f, colTipo)
                        + "' no válido (usa NC_MAYOR, NC_MENOR, OBSERVACION u OPORTUNIDAD_MEJORA)");
            }
            h.setTipo(t);

            String descripcion = Textos.limpio(celda(f, colDescripcion));
            if (descripcion == null) errores.add("Fila " + numeroFila + ": falta la descripción");
            h.setDescripcion(descripcion);

            String codigoControl = Textos.limpio(celda(f, colControl));
            if (codigoControl != null) {
                String iso = codigoControl.toUpperCase(Locale.ROOT).replace(" ", "");
                if (!iso.startsWith("A.")) iso = "A." + iso;
                Control control = controles.findByCodigoIso(iso).orElse(null);
                if (control == null) errores.add("Fila " + numeroFila + ": el control " + codigoControl + " no existe");
                h.setControl(control);
            }

            String fechaTexto = Textos.limpio(celda(f, colFecha));
            if (fechaTexto != null) {
                LocalDate fechaLimite = leerFecha(fechaTexto);
                if (fechaLimite == null) {
                    errores.add("Fila " + numeroFila + ": fecha '" + fechaTexto + "' no válida (usa 2026-12-31 o 31/12/2026)");
                }
                h.setFechaLimite(fechaLimite);
            }
            resultado.add(h);
        }

        if (!errores.isEmpty()) {
            List<String> primeros = errores.size() > 10 ? errores.subList(0, 10) : errores;
            throw error("No se importó nada. Corrige el archivo: " + String.join(" | ", primeros)
                    + (errores.size() > 10 ? " | ... y " + (errores.size() - 10) + " errores más" : ""));
        }
        return resultado;
    }

    /** Plantilla de ejemplo para que el usuario sepa cómo llenar el archivo. */
    public byte[] plantilla() {
        return Csv.generar(COLUMNAS, List.of(
                List.of("H-01", "NC_MENOR", "No hay evidencia de revisión periódica de accesos", "A.5.18", "2026-12-15"),
                List.of("H-02", "OBSERVACION", "La política de contraseñas no se ha comunicado al personal", "A.5.17", ""),
                List.of("H-03", "OPORTUNIDAD_MEJORA", "Automatizar las copias de seguridad del servidor de archivos", "A.8.13", "")));
    }

    // ================= HALLAZGOS =================

    @Transactional(readOnly = true)
    public List<Hallazgo> hallazgosDe(Long auditoriaId) {
        obtener(auditoriaId);
        return hallazgos.findByAuditoriaIdOrderByCodigoAsc(auditoriaId);
    }

    @Transactional(readOnly = true)
    public List<Hallazgo> buscarHallazgos(Hallazgo.Estado estado, Hallazgo.Tipo tipo) {
        return hallazgos.buscar(estado, tipo);
    }

    @Transactional
    public Hallazgo agregarHallazgo(Long auditoriaId, HallazgoRequest d) {
        Auditoria a = obtener(auditoriaId);
        Hallazgo h = new Hallazgo();
        h.setAuditoriaId(auditoriaId);
        String codigo = Textos.limpio(d.codigo());
        if (codigo == null) codigo = String.format("H-%02d", hallazgos.countByAuditoriaId(auditoriaId) + 1);
        if (hallazgos.existsByAuditoriaIdAndCodigo(auditoriaId, codigo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe el hallazgo " + codigo + " en esta auditoría");
        }
        h.setCodigo(codigo);
        copiarHallazgo(d, h);
        hallazgos.saveAndFlush(h);
        historial.registrar("HALLAZGO", h.getId(), a.getCodigo() + "/" + Textos.recortar(codigo, 10),
                Historial.Accion.CREAR, "Hallazgo " + codigo + " agregado a " + a.getCodigo());
        return h;
    }

    @Transactional
    public Hallazgo actualizarHallazgo(Long id, HallazgoRequest d) {
        Hallazgo h = hallazgos.findById(id).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el hallazgo con id " + id));
        Hallazgo.Estado antes = h.getEstado();
        copiarHallazgo(d, h);
        hallazgos.saveAndFlush(h);
        String detalle = antes != h.getEstado()
                ? "Hallazgo " + h.getCodigo() + ": " + antes + " -> " + h.getEstado()
                : "Hallazgo " + h.getCodigo() + " actualizado";
        historial.registrar("HALLAZGO", h.getId(), h.getCodigo(), Historial.Accion.EDITAR, detalle);
        return h;
    }

    private void copiarHallazgo(HallazgoRequest d, Hallazgo h) {
        Hallazgo.Estado estado = d.estado() != null ? d.estado() : h.getEstado();
        String accion = Textos.limpio(d.accionCorrectiva());
        if (estado == Hallazgo.Estado.CERRADO && accion == null) {
            throw error("Para cerrar un hallazgo, registra la acción correctiva");
        }
        Control control = null;
        if (d.controlId() != null) {
            control = controles.findById(d.controlId()).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                    error("No existe el control con id " + d.controlId()));
        }
        h.setTipo(d.tipo());
        h.setDescripcion(d.descripcion().trim());
        h.setControl(control);
        h.setResponsableId(d.responsableId());
        h.setEstado(estado);
        h.setFechaLimite(d.fechaLimite());
        h.setAccionCorrectiva(accion);
    }

    // ================= AYUDAS =================

    private Auditoria nuevaAuditoria(String nombre, Auditoria.Tipo tipo, LocalDate fecha, String auditor,
                                     String archivoOrigen) {
        Auditoria a = new Auditoria();
        a.setCodigo(String.format("AUD-%03d", repo.ultimoNumero(EmpresaActual.id()) + 1));
        a.setNombre(nombre.trim());
        a.setTipo(tipo);
        a.setFechaAuditoria(fecha);
        a.setAuditor(Textos.limpio(auditor));
        a.setArchivoOrigen(archivoOrigen);
        a.setImportadoPor(UsuarioActual.id());
        return repo.saveAndFlush(a);
    }

    private static String celda(List<String> fila, int columna) {
        return (columna < 0 || columna >= fila.size()) ? null : fila.get(columna);
    }

    private static LocalDate leerFecha(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(texto, FECHA_LATINA);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    /** Excel a veces guarda en UTF-8 y a veces en Windows-1252; aceptamos ambos. */
    private static String decodificar(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("windows-1252"));
        }
    }

    private static ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
