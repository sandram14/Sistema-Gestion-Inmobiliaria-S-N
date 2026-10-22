/**
 * DTOs de entrada: representan el JSON (o los parámetros) que envía el frontend.
 *
 * <p>Son records inmutables. Su contenido se valida en la capa service
 * ({@code ValidadorPropiedad}, {@code NormalizadorBusqueda}) antes de tocar la base.
 * Nunca se usan entidades JPA para recibir datos.</p>
 */
package py.sn.inmobiliaria.dto.request;
