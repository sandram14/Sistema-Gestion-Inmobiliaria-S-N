/**
 * Conversores entidad/proyección → DTO de respuesta.
 *
 * <p>Son funciones puras: no consultan la base. Los services los aplican dentro de la
 * transacción (por eso pueden recorrer relaciones LAZY). Ninguna entidad JPA sale del
 * backend: el navegador solo recibe estos DTOs.</p>
 */
package py.sn.inmobiliaria.mapper;
