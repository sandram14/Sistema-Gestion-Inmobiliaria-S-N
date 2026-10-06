# Panel administrativo — S&N Inmobiliaria

Aplicación React independiente del sitio público (`frontend/`). Tiene su propio login y sus
rutas protegidas; no hay enlaces a ella desde la web pública.

## Requisitos

- Node.js 20 o superior
- El backend (Tomcat) en `http://localhost:8080`
- Para ver en el panel las fotos originales (`/images/...`), el sitio público en `http://localhost:5173`

## Desarrollo

```powershell
cd admin
npm install      # solo la primera vez
npm run dev
```

Abrir `http://localhost:5174/admin/`. Vite reenvía `/api` y `/uploads` al backend, y `/images`
al sitio público (ver `vite.config.ts`; se pueden cambiar con `VITE_BACKEND_URL` y `VITE_FRONTEND_URL`).

## Compilar

```powershell
npm run build    # genera dist/ para publicar bajo /admin/ (Fase 16: nginx)
```

## Seguridad

La protección de rutas de React es solo para la navegación. La sesión es la cookie HttpOnly del
backend, y **cada operación** de `/api/admin/*` la vuelve a autorizar el backend (`AuthFilter`):
sin sesión válida responde 401 y el panel vuelve al login.

## Pantallas

| Ruta | Pantalla |
|------|----------|
| `/admin/login` | Inicio de sesión |
| `/admin/propiedades` | Listado con búsqueda, filtros por estado y tipo, edición y baja lógica |
| `/admin/propiedades/nueva` | Alta |
| `/admin/propiedades/:id` | Edición y fotos |

## Reglas que aplica el formulario (y que el backend vuelve a validar)

- Un terreno solo admite la operación Venta; una casa, Venta y/o Alquiler.
- El departamento, la ciudad y el barrio los determina el backend a partir del punto marcado
  en el mapa (`/api/admin/ubicacion`). Sin un punto dentro de un barrio registrado no se puede guardar.
- Fotos: JPEG, PNG o WebP de hasta 5 MB. El panel avisa antes de subir; el backend valida el
  contenido real del archivo.
- "Dar de baja" pasa la propiedad a Inactivo (baja lógica); se reactiva cambiando su estado.
