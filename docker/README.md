# Docker — S&N Inmobiliaria

Fase 4: servicio `db` (PostgreSQL 16 + PostGIS 3.4). Backend y frontend se agregan en la Fase 16.

## Primera vez

```powershell
Copy-Item docker\.env.example docker\.env     # luego editar las contraseñas
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml logs -f db   # esperar "[init] Base de datos inicializada correctamente"
```

## Uso diario

| Acción | Comando |
|---|---|
| Iniciar | `docker compose -f docker/docker-compose.yml up -d` |
| Estado | `docker compose -f docker/docker-compose.yml ps` |
| Consola SQL | `docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria` |
| Verificar reglas | `docker compose -f docker/docker-compose.yml exec db psql -U sn_app -d sn_inmobiliaria -f /database/pruebas/verificar.sql` |
| Detener (conserva los datos) | `docker compose -f docker/docker-compose.yml stop` |

Desde la PC, la base está en `localhost:5433` (usuario y contraseña de `docker/.env`).

## Recrear la base desde cero

Los scripts de `/database` solo se ejecutan cuando el volumen de datos está vacío.
Para volver a aplicarlos hay que **borrar el volumen, lo que elimina todos los datos**:

```powershell
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d
```
