/**
 * DAOs / Repositories: único lugar donde se escriben consultas, siempre parametrizadas.
 *
 * <p>Dos estilos, según lo que conviene a cada caso:</p>
 * <ul>
 *   <li><b>JPA/Hibernate</b> (Jpa*Dao): entidades y JPQL. Los métodos reciben el
 *       {@link jakarta.persistence.EntityManager} de la transacción que abrió el service,
 *       así varias operaciones de distintos DAOs quedan en una misma transacción
 *       (todo o nada).</li>
 *   <li><b>JDBC</b> (Jdbc*Dao): SQL nativo de PostGIS (geometrías y GeoJSON), que Hibernate
 *       no necesita conocer. Devuelven records livianos, no entidades.</li>
 * </ul>
 *
 * <p>Los DAOs no validan reglas de negocio: eso es tarea de la capa service.</p>
 */
package py.sn.inmobiliaria.repository;
