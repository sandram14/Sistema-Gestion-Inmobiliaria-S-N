# Integración: búsqueda por presupuesto en guaraníes

Desde la migración 009 y el backend 0.21.0, **el sistema trabaja solo en guaraníes (PYG)**: todos los precios
están en guaraníes y el presupuesto también. No hay selector de moneda ni conversión.

## Parámetros de `GET /api/propiedades`

| Parámetro | Tipo | Reglas |
|---|---|---|
| `presupuestoMin` | número | Mayor a 0. Opcional |
| `presupuestoMax` | número | Mayor a 0. Opcional. Si están los dos, el mínimo no puede ser mayor que el máximo |
| `moneda` | `PYG` | **Opcional, conviene no enviarlo.** Si se envía otro valor (por ejemplo `USD`), responde `400` |

Ejemplo: `/api/propiedades?operacion=Venta&presupuestoMin=300000000&presupuestoMax=900000000`

- Sin `presupuestoMin` ni `presupuestoMax`, la búsqueda no filtra por precio.
- Con `operacion`, el precio que se compara es el de **esa** operación (por ejemplo, el alquiler mensual).
  Sin `operacion`, una propiedad entra si **alguno** de sus precios está dentro del rango.
- Errores: `400` con `campos.presupuestoMin`, `campos.presupuestoMax` o `campos.moneda`, para mostrarlos junto a
  cada campo. Ejemplos de mensajes: "Debe ser mayor a 0", "No puede ser mayor que el presupuesto máximo" y
  "Solo se aceptan guaraníes (PYG)".
- La respuesta mantiene `"moneda": "PYG"` en cada propiedad y en cada operación, por compatibilidad.

## Cómo lo usa el sitio (W4)

| Elemento | Comportamiento |
|---|---|
| Campos | "Desde (Gs.)" y "Hasta (Gs.)", vacíos por defecto (vacío = sin límite). Un campo vacío o en 0 no se envía |
| Al escribir | Puntos de miles automáticos (`3.000.000`) y teclado numérico en el celular; máximo 13 cifras (`numeric(15,2)`) |
| Cuándo se aplica | Al pulsar **Buscar**, **Enter** o al **salir del campo**, no en cada tecla. Los demás filtros se aplican al instante |
| "Desde" mayor que "Hasta" | Error bajo el campo, sin consultar al backend |
| Sin resultados | Aviso: "No hay propiedades publicadas entre Gs. X y Gs. Y. Probá ampliar el presupuesto o quitar algún filtro." |
| Formato de precios | "Gs. 450.000.000" y "Gs. 4.000.000 / mes" (alquiler), con `services/guaranies.ts`, en tarjetas, detalle, mapa y ofertas |

## Presupuesto en el chat

El asistente no usa estos parámetros. En el recorrido guiado, el visitante escribe su presupuesto con sus palabras
("entre 300 y 500 millones") y queda tal cual en el resumen y en el mensaje de WhatsApp ("Omitir" registra
"No indicado"). En una pregunta libre ("casas de menos de 100.000.000 Gs."), Gemini recibe los precios en guaraníes
y la regla 9 de sus instrucciones le pide mencionar solo las propiedades que entran en ese presupuesto.
