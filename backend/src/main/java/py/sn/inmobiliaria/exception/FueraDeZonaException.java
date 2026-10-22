package py.sn.inmobiliaria.exception;

import jakarta.servlet.http.HttpServletResponse;

/**
 * El punto marcado en el mapa no está dentro de ningún barrio registrado (HTTP 404,
 * código FUERA_DE_ZONA). El código específico permite que el panel muestre un aviso claro
 * en lugar de un error genérico.
 */
public class FueraDeZonaException extends ApiException {

    private static final long serialVersionUID = 1L;

    public FueraDeZonaException() {
        super(HttpServletResponse.SC_NOT_FOUND, "FUERA_DE_ZONA",
                "El punto marcado está fuera de las zonas registradas. Marque una ubicación dentro de uno de "
                        + "los barrios disponibles en el mapa.");
    }
}
