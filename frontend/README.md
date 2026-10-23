# Vigentis — web (React)

Interfaz de Vigentis 1.0. Se conecta al backend de Spring Boot (carpeta `../backend`).

## Requisitos
- Node.js 20 o superior
- El backend corriendo en http://localhost:8080

## Primera vez
```
npm.cmd install
```

## Arrancar (desarrollo)
```
npm.cmd run dev
```
Abrir http://localhost:5173

Las llamadas a `/api` se envían al backend automáticamente (ver `vite.config.js`).

## Estructura
- `src/api.js` — conexión con la API y manejo del token de sesión
- `src/auth.jsx` — inicio y cierre de sesión, permisos del usuario
- `src/components/` — menú lateral y piezas reutilizables (modal, campos, estados)
- `src/pages/` — una pantalla por módulo
