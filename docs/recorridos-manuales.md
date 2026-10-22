# Recorridos manuales por rol

Lista para revisar el panel a mano antes de una entrega, con cada rol: qué hacer y qué se espera ver.
Los resultados se comprobaron en el entorno de pruebas el 19/10/2026 y, para 4.10 y 5.4, el 22/10/2026 (backend 0.25.0).

**Dónde hacerlos:** en el entorno de pruebas (`http://localhost:8081/admin/`, ver
[`pruebas.md`](pruebas.md), sección 3), nunca en el sistema real, porque se crean usuarios y propiedades.
Al detener el entorno con `down -v` desaparece todo.

La columna "✓" es para marcar el resultado durante la revisión.

---

## 0. Preparación (como administrador)

| # | Qué hacer | Qué esperar | ✓ |
|---|---|---|---|
| 0.1 | Entrar en `/admin/` con `PRUEBA_ADMIN_EMAIL` y `PRUEBA_ADMIN_PASSWORD` de `docker/.env.pruebas` | Pantalla de Inicio: "Buenas tardes, Administrador" (o "Buenos días" / "Buenas noches", según la hora) y una frase debajo | |
| 0.2 | Usuarios → Nuevo usuario: nombre "Gerente", apellido "Prueba", un email `@sn.test`, rol **Gerente** → Crear usuario | Se abre la edición del usuario con "Contraseña temporal: …". Anotarla: se muestra **una sola vez** | |
| 0.3 | Repetir con nombre "Vendedor" y rol **Vendedor** | Igual que 0.2 | |

## 1. Primer ingreso con contraseña temporal (gerente y vendedor)

| # | Qué hacer | Qué esperar | ✓ |
|---|---|---|---|
| 1.1 | En otra ventana privada, entrar con el email del gerente y su contraseña temporal | Se abre "Cambiar contraseña" con el aviso "Su cuenta tiene una contraseña temporal. Debe cambiarla para continuar usando el panel." | |
| 1.2 | Escribir en la dirección `/admin/propiedades` | Vuelve a "Cambiar contraseña": no se puede usar el panel sin cambiarla | |
| 1.3 | Completar la contraseña actual (la temporal) y una nueva de 10 caracteres o más, con letras y números, dos veces → Cambiar contraseña | Pasa a "Mi cuenta" con "Contraseña actualizada." | |
| 1.4 | El botón del ojo de cada campo de contraseña | Muestra y oculta lo escrito | |

Repetir 1.1 a 1.3 con el vendedor.

## 2. Administrador

| # | Qué hacer | Qué esperar | ✓ |
|---|---|---|---|
| 2.1 | Mirar el menú lateral | Inicio, Propiedades, Consultas IA, Gestión del sitio web, Usuarios y Mi cuenta | |
| 2.2 | Inicio | Cuatro fichas: clientes por contactar, Nueva propiedad, Nuevo usuario y Sitio web | |
| 2.3 | Propiedades → Nueva propiedad: título, habitaciones, baños, superficie, precio; clic en el mapa dentro de un barrio → Crear propiedad | El formulario muestra el barrio, la ciudad y el departamento del punto. Al crear: "Propiedad prop-0XX creada. Ahora puede agregar las fotos." | |
| 2.4 | En la edición, subir una foto JPG, PNG o WebP y marcarla como principal | La foto aparece en la galería con "Hacer principal" en las demás | |
| 2.5 | Abrir el sitio (`http://localhost:8081/buscar?propiedad=prop-0XX`) | La propiedad aparece con su título y el precio como "Gs. 350.000.000" (con el precio que se cargó) | |
| 2.6 | Editar cualquier propiedad (por ejemplo prop-001) | Botones "Guardar cambios" y, en las fotos, "Eliminar" y "Hacer principal". Se puede cambiar el usuario responsable | |
| 2.7 | Propiedades → en la fila de la propiedad creada en 2.3, "Dar de baja" (confirmar) | Pasa a "Inactivo" y deja de verse en el sitio; no se borra (se reactiva cambiando su estado) | |
| 2.8 | Usuarios → abrir el vendedor → Generar contraseña temporal (confirmar) | Se muestra una nueva contraseña temporal; las sesiones abiertas del vendedor se cierran (en su ventana, la siguiente acción lo lleva al inicio de sesión) | |
| 2.9 | Usuarios → abrir el vendedor → "Desactivar cuenta" (confirmar); luego "Reactivar cuenta" | El estado cambia en el listado. Mientras está desactivado no puede ingresar al panel | |
| 2.10 | Gestión del sitio web → Contacto: cambiar el horario → Guardar y publicar | El sitio (`/contacto`) muestra el horario nuevo | |
| 2.11 | Consultas IA → Historial, Análisis técnico y Clientes potenciales | Las tres pestañas abren. En Clientes potenciales se puede asignar un responsable | |
| 2.12 | Mi cuenta → tema Oscuro | El panel cambia a colores oscuros y lo recuerda en ese navegador | |

## 3. Gerente

| # | Qué hacer | Qué esperar | ✓ |
|---|---|---|---|
| 3.1 | Mirar el menú lateral | Igual que el administrador: Inicio, Propiedades, Consultas IA, Gestión del sitio web, Usuarios y Mi cuenta | |
| 3.2 | Inicio | Fichas: clientes por contactar, Nueva propiedad, Sitio web y Consultas IA | |
| 3.3 | Editar una propiedad de otro responsable (por ejemplo prop-001) | Puede guardar cambios y asignar responsable; en el listado tiene "Dar de baja" | |
| 3.4 | Gestión del sitio web | Puede editar y publicar las pestañas Inicio, Nosotros y Contacto | |
| 3.5 | Consultas IA | Ve las tres pestañas (Historial, Análisis técnico y Clientes potenciales), con todas las consultas y todos los clientes, y puede asignar responsables | |
| 3.6 | Usuarios | Ve el listado de usuarios, pero **solo para consultar** | |
| 3.7 | Escribir en la dirección `/admin/usuarios/nuevo` | "No tiene permisos para gestionar usuarios." | |

## 4. Vendedor

| # | Qué hacer | Qué esperar | ✓ |
|---|---|---|---|
| 4.1 | Mirar el menú lateral | Solo Inicio, Propiedades, Consultas IA y Mi cuenta (sin Gestión del sitio web ni Usuarios) | |
| 4.2 | Inicio | Fichas: clientes por contactar (solo cuenta los suyos), Nueva propiedad, Consultas IA y Listado de propiedades | |
| 4.3 | Propiedades → Nueva propiedad → crear una | Se crea a su nombre: él figura como responsable | |
| 4.4 | Abrir una propiedad de otro responsable (por ejemplo prop-001) | "Esta propiedad está asignada a otro responsable. Puede verla…" y **no** aparece "Guardar cambios". En el listado no tiene "Dar de baja" | |
| 4.5 | Consultas IA | Solo las pestañas Historial y Clientes potenciales. En el historial, las consultas generales y las de sus propiedades | |
| 4.6 | Clientes potenciales | Solo los asignados a él. Puede cambiar su estado de seguimiento, pero no asignar responsable | |
| 4.7 | Escribir en la dirección `/admin/consultas/analisis` | "No tiene permisos para esta operación" | |
| 4.8 | Escribir en la dirección `/admin/usuarios` | "No tiene permisos para esta operación" | |
| 4.9 | Escribir en la dirección `/admin/usuarios/nuevo` | "No tiene permisos para gestionar usuarios." | |
| 4.10 | Escribir en la dirección `/admin/sitio/inicio` (también `/nosotros` y `/contacto`) | "No tiene permisos para gestionar el sitio web." y ningún botón de guardar | |

## 5. Para todos los roles

| # | Qué hacer | Qué esperar | ✓ |
|---|---|---|---|
| 5.1 | Mi cuenta → cambiar el email (pide la contraseña) | El email nuevo queda guardado; con él se inicia sesión la próxima vez | |
| 5.2 | Menú → Cerrar sesión | Vuelve al inicio de sesión. La dirección `/admin/propiedades` pide iniciar sesión de nuevo | |
| 5.3 | En el celular (o con la ventana a 390 px) | El menú se abre con un botón y ningún texto se sale de su recuadro | |
| 5.4 | Contraseña incorrecta 5 veces seguidas | El inicio de sesión de ese email se bloquea 15 minutos **desde esa conexión** ("Demasiados intentos fallidos…"); desde otra conexión (otra IP) se puede entrar | |

## Al terminar

Detener y borrar el entorno de pruebas (borra los usuarios y propiedades creados en estos recorridos):

```powershell
docker compose -f docker/docker-compose.pruebas.yml --env-file docker/.env.pruebas down -v
```
