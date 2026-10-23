package com.vigentis.documentos;

import com.vigentis.comun.EmpresaActual;
import com.vigentis.comun.Textos;
import com.vigentis.controles.Control;
import com.vigentis.controles.ControlRepository;
import com.vigentis.historial.Historial;
import com.vigentis.historial.HistorialService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentoService {

    public record Descarga(Resource recurso, String nombre, String tipo) {}

    private static final Set<String> EXTENSIONES = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "txt", "csv", "png", "jpg", "jpeg");

    private final DocumentoRepository repo;
    private final ControlRepository controles;
    private final HistorialService historial;
    private final String directorio;
    private final JdbcTemplate jdbc;

    public DocumentoService(DocumentoRepository repo, ControlRepository controles, HistorialService historial,
                            @Value("${vigentis.archivos.directorio}") String directorio, JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.repo = repo;
        this.controles = controles;
        this.historial = historial;
        this.directorio = directorio;
    }

    @Transactional(readOnly = true)
    public List<Documento> buscar(String texto, Documento.Tipo tipo, Documento.Estado estado) {
        return repo.buscar(Textos.limpio(texto), tipo, estado);
    }

    @Transactional(readOnly = true)
    public Documento obtener(Long id) {
        return repo.findById(id).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el documento con id " + id));
    }

    @Transactional
    public Documento crear(DocumentoRequest d) {
        Documento doc = new Documento();
        doc.setCodigo(String.format("DOC-%03d", repo.ultimoNumero(EmpresaActual.id()) + 1));
        doc.setTitulo(d.titulo().trim());
        doc.setTipo(d.tipo());
        String version = Textos.limpio(d.version());
        if (version != null) doc.setVersion(version);
        doc.setResponsableId(d.responsableId());
        doc.setFechaRevision(d.fechaRevision());
        repo.saveAndFlush(doc);
        historial.registrar("DOCUMENTO", doc.getId(), doc.getCodigo(), Historial.Accion.CREAR,
                "Documento creado: " + doc.getTitulo() + " (" + doc.getTipo() + ")");
        return doc;
    }

    @Transactional
    public Documento actualizar(Long id, DocumentoRequest d) {
        Documento doc = obtener(id);
        Documento.Estado nuevo = d.estado() != null ? d.estado() : doc.getEstado();
        if (nuevo == Documento.Estado.APROBADO && doc.getEstado() != Documento.Estado.APROBADO) {
            throw error("Para aprobar un documento usa la acción 'aprobar'");
        }
        if (nuevo == Documento.Estado.BORRADOR && doc.getEstado() == Documento.Estado.APROBADO) {
            doc.setFechaAprobacion(null);
        }
        doc.setTitulo(d.titulo().trim());
        doc.setTipo(d.tipo());
        String version = Textos.limpio(d.version());
        if (version != null) doc.setVersion(version);
        doc.setEstado(nuevo);
        doc.setResponsableId(d.responsableId());
        doc.setFechaRevision(d.fechaRevision());
        repo.saveAndFlush(doc);
        historial.registrar("DOCUMENTO", doc.getId(), doc.getCodigo(), Historial.Accion.EDITAR,
                "Documento actualizado: " + doc.getTitulo() + " (v" + doc.getVersion() + ", " + doc.getEstado() + ")");
        return doc;
    }

    @Transactional
    public Documento aprobar(Long id) {
        Documento doc = obtener(id);
        if (doc.getEstado() == Documento.Estado.APROBADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El documento ya está aprobado");
        }
        if (doc.getEstado() == Documento.Estado.OBSOLETO) {
            throw error("Un documento obsoleto no se puede aprobar");
        }
        if (!doc.isTieneArchivo()) {
            throw error("Sube el archivo del documento antes de aprobarlo");
        }
        doc.setEstado(Documento.Estado.APROBADO);
        doc.setFechaAprobacion(LocalDate.now());
        if (doc.getFechaRevision() == null) {
            doc.setFechaRevision(LocalDate.now().plusYears(1));   // revisión anual por defecto
        }
        repo.saveAndFlush(doc);
        historial.registrar("DOCUMENTO", doc.getId(), doc.getCodigo(), Historial.Accion.EDITAR,
                "Documento aprobado: " + doc.getTitulo() + " v" + doc.getVersion());
        return doc;
    }

    // ================= ARCHIVOS =================

    @Transactional
    public Documento subirArchivo(Long id, MultipartFile archivo) {
        Documento doc = obtener(id);
        if (archivo == null || archivo.isEmpty()) throw error("Adjunta un archivo");
        if (doc.getEstado() == Documento.Estado.OBSOLETO) {
            throw error("No se puede subir un archivo a un documento obsoleto");
        }
        String original = nombreSeguro(archivo.getOriginalFilename());
        String extension = extension(original);
        if (!EXTENSIONES.contains(extension)) {
            throw error("Tipo de archivo no permitido. Se aceptan: " + String.join(", ", EXTENSIONES.stream().sorted().toList()));
        }

        // El contenido se guarda en la base de datos (tabla documento_archivos), así no se pierde
        // aunque el servidor se reinicie. archivo_ruta solo marca dónde está.
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el archivo");
        }
        jdbc.update("INSERT INTO documento_archivos (documento_id, contenido) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE contenido = VALUES(contenido)", doc.getId(), contenido);
        String nombreInterno = "bd:" + doc.getId();

        String detalle;
        if (doc.getEstado() == Documento.Estado.APROBADO) {
            // Cambiar el archivo de un documento aprobado crea una nueva versión que debe aprobarse otra vez
            String anterior = doc.getVersion();
            doc.setVersion(siguienteVersion(anterior));
            doc.setEstado(Documento.Estado.BORRADOR);
            doc.setFechaAprobacion(null);
            detalle = "Nueva versión " + doc.getVersion() + " (antes " + anterior + "), pendiente de aprobación: " + original;
        } else {
            detalle = "Archivo cargado: " + original;
        }
        doc.setArchivoNombre(Textos.recortar(original, 255));
        doc.cambiarRutaInterna(nombreInterno);
        doc.setArchivoTipo(Textos.recortar(archivo.getContentType(), 100));
        doc.setArchivoTamano(archivo.getSize());
        repo.saveAndFlush(doc);
        historial.registrar("DOCUMENTO", doc.getId(), doc.getCodigo(), Historial.Accion.EDITAR, detalle);
        return doc;
    }

    @Transactional(readOnly = true)
    public Descarga descargar(Long id) {
        Documento doc = obtener(id);
        if (doc.rutaInterna() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El documento todavía no tiene archivo");
        }
        if (doc.rutaInterna().startsWith("bd:")) {
            List<byte[]> filas = jdbc.queryForList(
                    "SELECT contenido FROM documento_archivos WHERE documento_id = ?", byte[].class, doc.getId());
            if (filas.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el archivo en la base de datos");
            }
            return new Descarga(new ByteArrayResource(filas.get(0)), doc.getArchivoNombre(), doc.getArchivoTipo());
        }
        // Archivos antiguos guardados en disco (antes de la versión con base de datos)
        Path carpeta = carpeta();
        Path archivo = carpeta.resolve(doc.rutaInterna()).normalize();
        if (!archivo.startsWith(carpeta) || !Files.isRegularFile(archivo)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el archivo en el servidor");
        }
        return new Descarga(new FileSystemResource(archivo), doc.getArchivoNombre(), doc.getArchivoTipo());
    }

    // ================= CONTROLES QUE EVIDENCIA =================

    @Transactional(readOnly = true)
    public List<Control> controlesDe(Long documentoId) {
        obtener(documentoId);
        return controles.findAllById(repo.idsControles(documentoId)).stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Documento> documentosDeControl(Long controlId) {
        if (controles.findById(controlId).filter(EmpresaActual::esDeLaEmpresa).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el control con id " + controlId);
        }
        return repo.buscarPorControl(controlId);
    }

    @Transactional
    public Control vincularControl(Long documentoId, Long controlId) {
        Documento doc = obtener(documentoId);
        Control c = controles.findById(controlId).filter(EmpresaActual::esDeLaEmpresa).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el control con id " + controlId));
        if (repo.contarVinculo(documentoId, controlId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese control ya está vinculado al documento");
        }
        repo.vincular(documentoId, controlId);
        historial.registrar("DOCUMENTO", doc.getId(), doc.getCodigo(), Historial.Accion.EDITAR,
                "Documento vinculado como evidencia del control " + c.getCodigoIso());
        return c;
    }

    @Transactional
    public void desvincularControl(Long documentoId, Long controlId) {
        Documento doc = obtener(documentoId);
        if (repo.desvincular(documentoId, controlId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ese control no estaba vinculado al documento");
        }
        historial.registrar("DOCUMENTO", doc.getId(), doc.getCodigo(), Historial.Accion.EDITAR,
                "Documento desvinculado del control con id " + controlId);
    }

    // ================= AYUDAS =================

    private Path carpeta() {
        return Paths.get(directorio).toAbsolutePath().normalize();
    }

    /** Quita cualquier ruta que venga en el nombre (por ejemplo C:\carpeta\archivo.pdf). */
    private static String nombreSeguro(String nombre) {
        if (nombre == null || nombre.isBlank()) return "archivo";
        String n = nombre.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).trim();
        return n.isEmpty() ? "archivo" : n;
    }

    private static String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    /** "1.0" -> "2.0", "3.2" -> "4.0". */
    private static String siguienteVersion(String version) {
        try {
            String mayor = version.contains(".") ? version.substring(0, version.indexOf('.')) : version;
            return (Integer.parseInt(mayor.trim()) + 1) + ".0";
        } catch (RuntimeException e) {
            return Textos.recortar(version + "+", 10);
        }
    }

    private static ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
