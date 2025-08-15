package org.interkambio.SistemaInventarioBackend.service;

import org.interkambio.SistemaInventarioBackend.DTO.BookStockLocationDTO;
import org.interkambio.SistemaInventarioBackend.criteria.BookSearchCriteria;
import org.interkambio.SistemaInventarioBackend.criteria.BookStockLocationSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookStockLocationService extends GenericService<BookStockLocationDTO, Long> {
    public Page<BookStockLocationDTO> searchLocations(BookStockLocationSearchCriteria criteria, Pageable pageable);
}

