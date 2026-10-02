package py.sn.inmobiliaria.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import py.sn.inmobiliaria.mapper.CatalogoMapper;
import py.sn.inmobiliaria.service.CatalogoService;

import java.io.IOException;

/**
 * {@code GET /api/catalogos}: tipos, estados, operaciones, monedas, operaciones permitidas
 * por tipo y el árbol Departamento → Ciudad → Barrio. Lo usan los formularios.
 */
@WebServlet(name = "CatalogoServlet", urlPatterns = "/api/catalogos")
public class CatalogoServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        escribirJson(resp, HttpServletResponse.SC_OK, CatalogoMapper.dto(servicio(CatalogoService.class).obtener()));
    }
}
