package org.interkambio.SistemaInventarioBackend.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.interkambio.SistemaInventarioBackend.DTO.InventoryTransactionDTO;
import org.interkambio.SistemaInventarioBackend.criteria.InventoryTransactionSearchCriteria;
import org.interkambio.SistemaInventarioBackend.mapper.InventoryTransactionMapper;
import org.interkambio.SistemaInventarioBackend.model.Book;
import org.interkambio.SistemaInventarioBackend.model.InventoryTransaction;
import org.interkambio.SistemaInventarioBackend.model.TransactionType;
import org.interkambio.SistemaInventarioBackend.repository.BookRepository;
import org.interkambio.SistemaInventarioBackend.repository.BookStockLocationRepository;
import org.interkambio.SistemaInventarioBackend.repository.InventoryTransactionRepository;
import org.interkambio.SistemaInventarioBackend.repository.UserRepository;
import org.interkambio.SistemaInventarioBackend.service.InventoryTransactionService;
import org.interkambio.SistemaInventarioBackend.specification.InventoryTransactionSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryTransactionServiceImpl implements InventoryTransactionService {

    private final InventoryTransactionRepository repository;
    private final InventoryTransactionMapper mapper;
    private final BookStockLocationRepository bookStockLocationRepository; // para actualizar stock
    private final BookRepository bookRepository; // para validar existencia de libro
    private final UserRepository userRepository; // validar usuario

    @Override
    public Page<InventoryTransactionDTO> searchTransactions(InventoryTransactionSearchCriteria criteria, Pageable pageable) {
        InventoryTransactionSpecification spec = new InventoryTransactionSpecification();
        return repository.findAll(spec.build(criteria), pageable)
                .map(mapper::toDTO);
    }

    @Override
    public InventoryTransactionDTO createTransaction(InventoryTransactionDTO dto) {

        // Validar tipo de transacción
        TransactionType transactionType;
        try {
            transactionType = TransactionType.valueOf(dto.getTransactionType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de transacción inválido: " + dto.getTransactionType());
        }

        // Validar existencia de libro
        var bookOpt = bookRepository.findBySku(dto.getBookSku());
        if (bookOpt.isEmpty()) {
            throw new EntityNotFoundException("Libro no encontrado con SKU: " + dto.getBookSku());
        }
        Book book = bookOpt.get();  // ENTIDAD GESTIONADA

        // Validar usuario
        if (dto.getUserId() != null) {
            if (!userRepository.existsById(dto.getUserId())) {
                throw new EntityNotFoundException("Usuario no encontrado con ID: " + dto.getUserId());
            }
        }

        // Validaciones para ubicaciones según tipo
        switch (transactionType) {
            case PURCHASE:
                if (dto.getToLocationId() == null) {
                    throw new IllegalArgumentException("Compra debe tener ubicación destino (toLocationId)");
                }
                break;
            case SALE:
                if (dto.getFromLocationId() == null) {
                    throw new IllegalArgumentException("Venta debe tener ubicación origen (fromLocationId)");
                }
                break;
            case TRANSFER:
                if (dto.getFromLocationId() == null || dto.getToLocationId() == null) {
                    throw new IllegalArgumentException("Transferencia debe tener ambas ubicaciones (fromLocationId y toLocationId)");
                }
                break;
            default:
                // Otras validaciones que quieras añadir
                break;
        }

        // Actualizar stock: lógica simplificada, luego puedes mejorar
        switch (transactionType) {
            case PURCHASE:
                // Incrementar stock en ubicación destino
                adjustStock(dto.getToLocationId(), dto.getQuantity());
                break;
            case SALE:
                // Reducir stock en ubicación origen
                adjustStock(dto.getFromLocationId(), -dto.getQuantity());
                break;
            case TRANSFER:
                // Reducir stock en ubicación origen
                adjustStock(dto.getFromLocationId(), -dto.getQuantity());
                // Incrementar stock en ubicación destino
                adjustStock(dto.getToLocationId(), dto.getQuantity());
                break;
            case ADJUSTMENT:
                // Ajuste manual: si fromLocationId definido, reducir; si toLocationId definido, incrementar
                if (dto.getFromLocationId() != null) {
                    adjustStock(dto.getFromLocationId(), -dto.getQuantity());
                }
                if (dto.getToLocationId() != null) {
                    adjustStock(dto.getToLocationId(), dto.getQuantity());
                }
                break;
            case RETURN_IN:
                // Incrementa stock en toLocationId
                if (dto.getToLocationId() != null) {
                    adjustStock(dto.getToLocationId(), dto.getQuantity());
                }
                break;
            case RETURN_OUT:
                // Reduce stock en fromLocationId
                if (dto.getFromLocationId() != null) {
                    adjustStock(dto.getFromLocationId(), -dto.getQuantity());
                }
                break;
        }

        // Mapear y guardar la transacción
        InventoryTransaction entity = mapper.toEntity(dto);
        // Aquí asignar el libro gestionado para evitar error transient
        entity.setBook(book);
        entity.setTransactionType(transactionType);
        InventoryTransaction saved = repository.save(entity);

        return mapper.toDTO(saved);
    }

    private void adjustStock(Long locationId, int quantityChange) {
        if (locationId == null) return;

        var locationOpt = bookStockLocationRepository.findById(locationId);
        if (locationOpt.isEmpty()) {
            throw new EntityNotFoundException("Ubicación no encontrada con ID: " + locationId);
        }

        var location = locationOpt.get();
        int nuevoStock = location.getStock() + quantityChange;
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("Stock insuficiente en ubicación ID: " + locationId);
        }

        location.setStock(nuevoStock);
        bookStockLocationRepository.save(location);
    }
}
