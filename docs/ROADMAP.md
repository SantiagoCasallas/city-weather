# Entregas

## 1 · Consulta pública completa — implementada

Angular y Spring Boot en `frontend/` y `backend/`. Búsqueda, selección inequívoca, condiciones actuales y siete días desde mañana, unidades y zona local. Validación, timeouts, caché limitada, presupuesto externo, errores accesibles, pruebas con proveedor simulado, CI y configuración Docker/Render.

Criterio: poder iniciar ambos proyectos y recorrer búsqueda → selección → clima, sin datos ficticios en ejecución normal. Las pruebas automatizadas no dependen del proveedor real. El despliegue público y la creación del repositorio GitHub no se dan por realizados.

## 2 · Identidad y PostgreSQL — pendiente

- Spring Security, registro e inicio/cierre de sesión; contraseñas con BCrypt/Argon2 y restricciones de contraseña y correo normalizado único.
- Sesiones con cookies HttpOnly, Secure en producción y SameSite; protección CSRF en escrituras y límites de intentos de autenticación. Evitar tokens persistidos en localStorage.
- PostgreSQL y migraciones Flyway; credenciales por entorno, nunca en Git. No usar H2 como sustituto de la prueba de PostgreSQL.
- Pruebas de integración con PostgreSQL real mediante Testcontainers.

Criterio: registro y autenticación persistentes, credenciales inválidas rechazadas, cierre de sesión efectivo y endpoints privados con 401 sin sesión.

## 3 · Favoritos e historial — pendiente

- Favoritos únicos por `(user_id, city_id)` con metadatos de la ciudad y coordenadas; alta, listado y eliminación.
- Historial solo de consultas meteorológicas exitosas: usuario, ciudad y fecha UTC; no guardar respuestas del proveedor innecesariamente.
- Paginación con tamaño máximo 50, orden estable por `queried_at DESC, id DESC` e índices por usuario y fecha.
- Cada consulta y modificación obtiene el usuario del contexto autenticado. Nunca confiar en un `userId` enviado por el cliente. Repositorios filtran por propietario; 404 para recursos ajenos.
- Integración con dos usuarios: B no puede leer, modificar ni eliminar datos de A; comprobar también intentos con IDs adivinados y páginas fuera de rango.

Criterio: pruebas de aislamiento de usuarios y persistencia aprobadas; UI de favoritos e historial con estados vacíos y paginación real.

## 4 · Portafolio y operación — pendiente

Publicar demo y revisar accesibilidad con lector de pantalla y límites del hosting. Conectar PostgreSQL persistente para las entregas privadas, configurar backups y revisar consumo, licencias y cabeceras de seguridad. No presentar una base gratuita temporal como almacenamiento permanente.
