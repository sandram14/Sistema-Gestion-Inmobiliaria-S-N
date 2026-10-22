package py.sn.inmobiliaria.dto.request;

/**
 * Registro de una imagen ya almacenada (la subida del archivo se implementa en la Fase 11).
 *
 * @param url         ruta relativa (/uploads/prop-017/abc.jpg) o URL https
 * @param descripcion texto alternativo opcional
 */
public record ImagenRequest(String url, String descripcion) {
}
