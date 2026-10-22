/**
 * Entidades JPA/Hibernate: una clase por tabla de PostgreSQL.
 *
 * <p>Criterios comunes:</p>
 * <ul>
 *   <li>Solo anotaciones estándar de Jakarta Persistence (sin dependencias de Hibernate).</li>
 *   <li>Relaciones {@code @ManyToOne} siempre LAZY: se cargan solo cuando se usan.</li>
 *   <li>El esquema lo crean los scripts de /database; Hibernate solo lo valida al arrancar.</li>
 *   <li>{@code columnDefinition} indica el tipo real de PostgreSQL (bpchar, timestamptz)
 *       cuando no coincide con el que Hibernate asumiría para el tipo Java; así la
 *       validación del esquema compara contra el tipo correcto.</li>
 *   <li>Las columnas geométricas (barrio.geom, ciudad.centro, propiedad.ubicacion) no se
 *       mapean: se leen con SQL nativo PostGIS en los DAOs.</li>
 *   <li>Nunca se envían al navegador: siempre pasan por un DTO.</li>
 * </ul>
 */
package py.sn.inmobiliaria.model;
