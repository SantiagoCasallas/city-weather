# Despliegue

## Render: primera entrega

1. Crea un repositorio GitHub independiente `city-weather` y sube los archivos. El entorno de trabajo actual no proporciona un `.git` editable, por lo que no se presupone ningún commit o push.
2. En Render, New → Blueprint y selecciona el repositorio. Revisa `render.yaml`: servicio Docker, plan `free`, raíz del repositorio.
3. Render construye el frontend y backend y asigna `PORT`. La app escucha en ese puerto y sirve `/` y `/api` en el mismo dominio.
4. Comprueba `/api/health` y una consulta real desde el navegador. Si el proveedor falla, la interfaz debe mostrar error; nunca reemplazarlo con un fixture.

También puedes elegir New Web Service → Docker → Free manualmente. Ninguna API key es necesaria para Open-Meteo gratuito no comercial. No configures las URLs del proveedor hacia hosts no confiables.

Variables: `PORT` (8080), `PROVIDER_TIMEOUT_MS` (5000), `CACHE_TTL_SECONDS` (600), `CACHE_MAX_ENTRIES` (500). El texto de la interfaz documenta la caché predeterminada de 10 minutos: si cambias el TTL, actualiza también ese texto. `GEOCODING_URL` y `FORECAST_URL` son para pruebas o infraestructura controlada; no son parámetros públicos.

## Límites gratuitos verificados

Revisado el 28 de octubre de 2026 en [Render Free](https://render.com/docs/free). El servicio se suspende tras 15 minutos sin tráfico; despertar puede tardar alrededor de un minuto. Hay 750 horas gratuitas por workspace/mes y límites de compilación y transferencia. El sistema de archivos es efímero y la caché se pierde al reiniciar. No hay garantía de disponibilidad y el plan no está destinado a producción.

La primera versión no necesita PostgreSQL. **Render Postgres gratuito caduca a los 30 días**, no tiene backups y no es una solución persistente para favoritos e historial. En la entrega de cuentas se elegirá un PostgreSQL con un plan gratuito vigente y adecuado, revisando sus condiciones en ese momento. No se ha provisionado una base ni un servicio.

Una sola instancia mantiene su presupuesto saliente en memoria. En un reinicio o con otras apps en la misma IP el límite local deja de representar el consumo total; hay que vigilar la cuota del proveedor y evitar escalar instancias sin un contador compartido. Las cargas de caché se serializan por tipo de consulta: apropiado para una demo pequeña; un tráfico mayor requiere coalescencia por clave y control de concurrencia.
