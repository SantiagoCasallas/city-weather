# Verificación de la entrega 1

Ejecutada el 28 de octubre de 2026 en el entorno de desarrollo.

| Comprobación | Resultado |
| --- | --- |
| Java 21, Spring Boot 3.5.16: `mvn verify` | 9 pruebas aprobadas, JAR generado |
| Angular 21: `npm test` | 5 pruebas aprobadas |
| Angular: `npm run build` | Compilación de producción aprobada; aproximadamente 216 kB sin comprimir |
| `npm run test:e2e` | 4 pruebas aprobadas: escritorio y móvil, Angular → Spring Boot → proveedor simulado |
| Consulta real Bogotá a través de Spring Boot | Geocodificación y clima recibidos de Open-Meteo; un timeout inicial y recuperación al reintentar |
| Revisión visual | Capturas de escritorio y 390 px; sin desbordamiento horizontal |

Los datos de las capturas son una consulta real de ese día. Los fixtures están aislados en los tests Java y `frontend/e2e/provider.py`. Los scripts normales y Docker usan los hosts oficiales.

No verificado: construcción de imagen Docker (Docker no disponible en este entorno), ejecución de GitHub Actions en GitHub y despliegue en Render. No se han creado servicios, repositorio remoto, cuentas ni base de datos. No se ha realizado una auditoría completa con lector de pantalla.
