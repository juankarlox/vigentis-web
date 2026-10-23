# Vigentis 1.0

Sistema web de gestión de ciberseguridad basado en **ISO/IEC 27001:2022**.
Permite registrar y relacionar los activos, riesgos, controles, incidentes, auditorías y documentos de seguridad de una organización, con trazabilidad de cada cambio.

> Vigentis **apoya** la gestión de un SGSI tomando la norma como referencia. No certifica organizaciones ni el software está certificado.

**Úsalo en línea:** https://vigentis.onrender.com (la primera carga puede tardar ~1 minuto si el servidor estaba dormido).

Proyecto Integrador I · Ingeniería de Sistemas · Universidad de Santander (Cúcuta) · Juan Carlos Arévalo Trujillo

## Módulos

| Módulo | Qué hace | Referencia ISO/IEC 27001 |
|---|---|---|
| Activos | Inventario con valoración de confidencialidad, integridad y disponibilidad; criticidad calculada | A.5.9, A.5.12 |
| Riesgos | Amenaza y vulnerabilidad, probabilidad × impacto (1–5), tratamiento, riesgo residual, aceptación formal por el dueño del riesgo, revisión periódica y bitácora | 6.1.2, 6.1.3, 8.2, 8.3 |
| Criterios de riesgo | La organización define sus escalas, los umbrales de cada nivel, el nivel aceptable y cada cuánto se revisan los riesgos | 6.1.2 a |
| Controles | Los 93 controles del Anexo A: aplica / justificación / estado / responsable (Declaración de Aplicabilidad) | 6.1.3 d |
| Incidentes | Registro, atención y cierre con acciones tomadas | A.5.24 – A.5.28 |
| Auditorías | Importación de hallazgos desde CSV y seguimiento hasta su cierre | 9.2, 10.2 |
| Documentos | Políticas, procedimientos y evidencias con versión, aprobación y revisión | 7.5 |
| Usuarios y roles | Roles configurables por la empresa, permisos por módulo, bloqueo por intentos fallidos | A.5.15, A.5.18, A.8.5 |
| Historial y reportes | Quién hizo qué y cuándo; exportación a CSV | A.8.15 |

En la versión 1.0 la información se registra o se importa manualmente: no hay agente instalado en los equipos ni análisis automático.

## Tecnologías

- **Frontend:** React 19 + Vite (`frontend/`)
- **Backend:** Java 25 + Spring Boot 4, Spring Security con JWT, Spring Data JPA (`backend/`)
- **Base de datos:** MySQL 8, versionada con Flyway (`backend/src/main/resources/db/migration`)

## Cómo usarlo

Doble clic en **`Iniciar Vigentis.bat`** (la primera vez crea el acceso **Vigentis** en el Escritorio).
Se abre el navegador en http://localhost:8080 cuando la aplicación está lista. Para apagarla, se cierra la ventana *Vigentis* de la barra de tareas.

La web de React está compilada dentro del backend (`backend/src/main/resources/static`), así que Vigentis es **una sola aplicación**.

Requisitos en el equipo: Java 25, MySQL 8 encendido con la base `vigentis` y el usuario `vigentis_app`, y estas variables de entorno (nunca se escriben en el código):

| Variable | Para qué |
|---|---|
| `DB_PASSWORD` | Contraseña del usuario `vigentis_app` de MySQL |
| `JWT_SECRET` | Clave para firmar las sesiones (mínimo 32 caracteres) |

La primera vez se crea el usuario `admin@vigentis.local` y su contraseña temporal aparece en la consola del backend.

### Desarrollo

```powershell
# Backend (crea y actualiza las tablas solo, con Flyway)
cd backend
.\mvnw.cmd spring-boot:run

# Frontend con recarga en vivo (otra ventana) -> http://localhost:5173
cd frontend
npm.cmd install
npm.cmd run dev

# Al terminar cambios en la web: compilarla dentro del backend
npm.cmd run build
```

## Estructura

```
index.html, script.js, styles.css   Prototipo visual inicial (publicado en GitHub Pages: juankarlox.github.io/vigentis-web)
backend/    API REST (Spring Boot): un paquete por módulo
frontend/   Interfaz web (React)
database/   Script inicial usado durante el diseño de la base de datos
ejemplos/   Archivos de prueba (CSV de hallazgos, documento de ejemplo)
```
