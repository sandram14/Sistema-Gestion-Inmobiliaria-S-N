# Integración: formulario de clientes potenciales en el chat público

Contrato del endpoint para el sitio público (`frontend/`). El backend ya está implementado (versión 0.17.0).

## Cuándo mostrar el formulario

- Cuando la respuesta de `POST /api/asistente/consultas` trae `sugerirWhatsApp: true`, junto al botón de WhatsApp.
- Cuando el visitante lo pide (por ejemplo, un botón "Quiero que me contacten" en el chat).

No se debe pedir datos en ningún otro momento ni hacer obligatorio el formulario para usar el chat.

## Endpoint

`POST /api/clientes-potenciales` · `Content-Type: application/json` · enviar con `credentials: 'include'`
(la cookie anónima `sn_sesion` permite vincular la última consulta del visitante; no es obligatoria).

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `nombre` | string | Sí | 2 a 120 caracteres |
| `telefono` | string | Uno de los dos | 6 a 30 caracteres: dígitos, espacios, `+`, `-`, `(` `)` |
| `email` | string | Uno de los dos | Formato de email, máximo 120 |
| `mensaje` | string | No | Máximo 1000 caracteres ("¿Qué le interesa?") |
| `codigoPropiedad` | string | No | Código de la propiedad que se está consultando (`propiedadEnConsulta.codigo`) |
| `aceptaTratamiento` | boolean | Sí | Debe ser `true` (casilla marcada por el visitante, nunca premarcada) |
| `versionConsentimiento` | string | Sí | Exactamente `"2026-10"` (versión del texto de abajo) |

Ejemplo:

```json
{ "nombre": "Ana Pérez", "telefono": "0981 123 456", "email": null, "mensaje": "Me interesa visitarla",
  "codigoPropiedad": "prop-004", "aceptaTratamiento": true, "versionConsentimiento": "2026-10" }
```

## Respuestas

| Estado | Cuerpo | Qué mostrar |
|---|---|---|
| `201` | `{ "registrado": true, "mensaje": "Gracias. Un asesor se pondrá en contacto con usted." }` | El `mensaje` |
| `400` | `{ "error": { "codigo": "VALIDACION", "mensaje": "...", "campos": { "nombre": "...", "contacto": "...", "aceptaTratamiento": "..." } } }` | Cada error junto a su campo (`contacto` = falta teléfono y email) |
| `429` | `{ "error": { "codigo": "DEMASIADAS_CONSULTAS", "mensaje": "..." } }` | El `mensaje` (límite: 3 envíos por hora por visitante) |
| `503` | `{ "error": { ... } }` | "No se pudo enviar. Intente más tarde o escríbanos por WhatsApp." |

La respuesta no devuelve ningún dato del cliente.

## Texto de aceptación (versión 2026-10)

> Acepto que S&N Inmobiliaria use mis datos (nombre y medio de contacto) únicamente para contactarme
> sobre esta consulta. Mis datos no se comparten con terceros.

Si se cambia el texto, hay que cambiar también la versión en el backend
(`ClientePotencialService.VERSION_CONSENTIMIENTO`) y en el formulario, para que quede registrado qué texto aceptó cada persona.

Este texto y la casilla **no garantizan por sí solos** el cumplimiento normativo: antes de usarlo con clientes reales,
conviene que alguien con conocimiento legal revise el texto, la política de privacidad del sitio y el plazo de conservación de los datos.

## Otro pendiente del chat (relacionado)

El asistente ya **no escribe los códigos** de las propiedades (`prop-001`) en sus respuestas. Pero el propio chat del sitio
todavía los muestra en tres mensajes suyos (`AssistantWidget.tsx`): "Escribí tu consulta sobre "…" (prop-001)…",
"…sobre la propiedad prop-001" y el texto de ayuda "Preguntá sobre prop-001…". Conviene reemplazarlos por el título de la propiedad.
El campo `propiedadesRelacionadas` de la respuesta del asistente sigue trayendo los códigos, para uso interno del sitio.
