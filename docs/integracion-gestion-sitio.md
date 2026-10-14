# Integración: contenido del sitio editable desde el panel

El panel (Gestión del sitio web, pestañas Inicio, Servicios, Nosotros y Contacto) guarda el contenido de la
portada, las ofertas destacadas, Servicios, Nosotros y Contacto. Para que esos cambios se vean en la web, los componentes del sitio deben leerlo de
`GET /api/sitio` en lugar de tenerlo escrito en el código. Backend: versión 0.18.0 (migración 006).

## Archivo listo para usar

`frontend/src/services/contenidoSitio.ts` (nuevo; no modifica ningún componente):

- `useContenidoSitio()` → `{ contenido, cargando }`. Devuelve de inmediato los textos actuales (respaldo) y el contenido real apenas llega.
- `obtenerContenidoSitio()` → la misma lectura como promesa (una sola petición por carga de página).
- `elegirOfertas(todas, contenido.ofertas)` → las propiedades de la portada: las seleccionadas en su orden o, si no hay selección, 6 al azar.
- Si el backend no responde, todo usa `CONTENIDO_RESPALDO` (los textos de hoy): el sitio nunca queda vacío.

## Qué reemplazar en cada componente

| Componente | Hoy (escrito en el código) | Usar |
|---|---|---|
| `home/HeroSection.tsx` | Imagen de fondo en `HeroSection.module.css`; texto "Más de 12.000 propiedades disponibles", título y subtítulo | `inicio.imagenPortada` (si no es `null`, como `background-image` en línea; si es `null`, la del CSS), `inicio.titulo`, `inicio.subtitulo`. **Quitar** el texto "Más de 12.000 propiedades disponibles" con su resaltado: se retiró del diseño |
| `home/OffersSection.tsx` | 6 propiedades al azar (`slice` sobre una copia mezclada) | `elegirOfertas(features, contenido.ofertas)` |
| `home/OffersSection.tsx`, bloque "Sobre nosotros" | Título, texto e imagen | **Quitar el bloque** de la portada: se retiró del diseño y ya no se edita desde el panel |
| `contacto/Contacto.tsx` | Tres agentes de ejemplo con datos inventados | Sección institucional: el mensaje **"Tu consulta nos interesa, ponte en contacto con nosotros"** y los canales `contacto.email` (`mailto:`), `contacto.telefono` (`tel:`, solo dígitos y `+`), `contacto.whatsapp` (`https://wa.me/{whatsapp}`), más `direccion`, `ciudad` y `horario` si no están vacíos |
| `layout/Footer.tsx` | Descripción de la empresa y "Ciudad del Este, Paraguay" | `contacto.descripcion`, `contacto.ciudad` |
| `chat/AssistantWidget.tsx` | Constante `AGENTE_WHATSAPP = '595981123456'` | `contacto.whatsapp` (con `obtenerContenidoSitio()`, al armar el enlace) |
| `servicios/Servicios.tsx` | Título, subtítulo, el arreglo `SERVICIOS` y el bloque inferior | `servicios.titulo`, `servicios.subtitulo`, `servicios.servicios` (1 a 8 elementos `{titulo, texto}`), `servicios.bloqueTitulo`, `servicios.bloqueTexto` |
| `nosotros/Nosotros.tsx` | Introducción, historia, cifras, `VALORES` y `RAZONES` | `nosotros.introduccion`, `historiaTitulo`, `historiaTexto`, `imagen` (si no es `null`), `cifras` (0 a 4), `valores` y `razones` (1 a 6 cada uno). Los íconos no se editan: asignarlos por posición (por ejemplo, `ICONOS[i % ICONOS.length]`), porque la cantidad de elementos puede cambiar |

**Datos provisionales:** mientras `contactoProvisional` sea `true`, el email, el teléfono o el WhatsApp son los de prueba
(`contacto@sn-inmobiliaria.example`, `+595 000 000 000`): el dominio `.example` no llega a nadie y el número no existe.
Conviene mostrar, por ejemplo, "Datos de contacto en actualización" en lugar de los enlaces mientras sea `true`.

## Formato de la respuesta

`GET /api/sitio` · público · `Cache-Control: public, max-age=60` (un cambio se ve en, como máximo, un minuto)

```json
{
  "inicio":   { "imagenPortada": "/uploads/sitio-001/….png", "titulo": "…", "subtitulo": "…" },
  "servicios": { "titulo": "…", "subtitulo": "…", "servicios": [{ "titulo": "…", "texto": "…" }], "bloqueTitulo": "…", "bloqueTexto": "…" },
  "nosotros": { "introduccion": "…", "historiaTitulo": "…", "historiaTexto": "…", "imagen": null,
                "cifras": [{ "valor": "+150", "etiqueta": "Propiedades" }], "valores": [{ "titulo": "…", "texto": "…" }], "razones": [ … ] },
  "contacto": { "nombreEmpresa": "…", "descripcion": "…", "direccion": "…", "ciudad": "…",
                "email": "…", "telefono": "…", "whatsapp": "595…", "horario": "…" },
  "contactoProvisional": true,
  "ofertas": ["prop-004", "prop-001"]
}
```

- Las imágenes son rutas `/uploads/…` del mismo sitio (nginx las sirve desde el backend).
- Hasta que alguien guarde la portada desde el panel, `inicio` puede traer también `etiqueta`, `nosotrosTitulo`, `nosotrosTexto` y `nosotrosImagen` (del contenido inicial): ignorarlos.
- `ofertas` contiene **solo propiedades disponibles**: si una destacada se vende o se da de baja, desaparece sola.
- Los textos son texto plano: mostrarlos como texto (no como HTML).

## Revisar antes de publicar

El contenido inicial incluye cifras que estaban escritas en el sitio y que conviene verificar desde el panel:
las de Nosotros (+150 propiedades, +12 barrios, +6 años).
La imagen de portada actual se carga desde un sitio externo (`turismoitaipu.com.br`): se recomienda subir una propia desde el panel.
