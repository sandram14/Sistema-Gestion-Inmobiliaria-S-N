# Integración: búsqueda por presupuesto en guaraníes o dólares

El backend (0.20.0) acepta un presupuesto **mínimo** además del máximo. El cliente elige la moneda y la búsqueda
muestra las propiedades cuyo precio está **publicado en esa moneda**: **no se convierten monedas**. Una propiedad
publicada en dólares no aparece si se busca en guaraníes, y viceversa. Conviene aclararlo junto al filtro
(por ejemplo: "Se muestran las propiedades publicadas en la moneda elegida").

## Parámetros de `GET /api/propiedades`

| Parámetro | Tipo | Reglas |
|---|---|---|
| `moneda` | `USD` o `PYG` | Moneda del presupuesto. Si se indica un presupuesto sin moneda, se usa `USD` |
| `presupuestoMin` | número | Mayor a 0. Opcional |
| `presupuestoMax` | número | Mayor a 0. Opcional. Si están los dos, el mínimo no puede ser mayor que el máximo |

Ejemplo: `/api/propiedades?operacion=Venta&moneda=PYG&presupuestoMin=300000000&presupuestoMax=900000000`

- Sin `presupuestoMin` ni `presupuestoMax`, la búsqueda no filtra por precio ni por moneda (como hasta ahora).
- Con `operacion`, el precio que se compara es el de **esa** operación (por ejemplo, el alquiler mensual).
- Errores: `400` con `campos.presupuestoMin`, `campos.presupuestoMax` o `campos.moneda`, para mostrarlos junto a cada campo.

## Sugerencia de interfaz

Selector de moneda (Guaraníes / Dólares) y dos campos "Desde" y "Hasta", vacíos por defecto (vacío = sin límite).
No enviar un campo vacío ni un 0: omitirlo.
