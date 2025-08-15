package org.interkambio.SistemaInventarioBackend.repository;

import org.interkambio.SistemaInventarioBackend.model.BookStockLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BookStockLocationRepository extends
        JpaRepository<BookStockLocation, Long>,
        JpaSpecificationExecutor<BookStockLocation> {

    /**
     * Cargar la entidad Book y Warehouse en el mismo fetch para evitar N+1.
     * NOTA: no incluir columnas escalares como bookcase o bookcaseFloor aquí.
     */
    @EntityGraph(attributePaths = {
            "warehouse",
            "book"
    })
    Page<BookStockLocation> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
            "warehouse",
            "book"
    })
    Page<BookStockLocation> findAll(Specification<BookStockLocation> spec, Pageable pageable);
}
