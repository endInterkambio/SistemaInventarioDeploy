package org.interkambio.SistemaInventarioBackend.service.impl;

import org.interkambio.SistemaInventarioBackend.DTO.BookStockLocationDTO;
import org.interkambio.SistemaInventarioBackend.criteria.BookStockLocationSearchCriteria;
import org.interkambio.SistemaInventarioBackend.mapper.BookStockLocationMapper;
import org.interkambio.SistemaInventarioBackend.model.Book;
import org.interkambio.SistemaInventarioBackend.model.BookCondition;
import org.interkambio.SistemaInventarioBackend.model.BookStockLocation;
import org.interkambio.SistemaInventarioBackend.model.LocationType;
import org.interkambio.SistemaInventarioBackend.repository.BookRepository;
import org.interkambio.SistemaInventarioBackend.repository.BookStockLocationRepository;
import org.interkambio.SistemaInventarioBackend.service.BookStockLocationService;
import org.interkambio.SistemaInventarioBackend.specification.BookStockLocationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookStockLocationServiceImpl
        extends GenericServiceImpl<BookStockLocation, BookStockLocationDTO, Long>
        implements BookStockLocationService {

    private final BookStockLocationRepository repository;
    private final BookStockLocationMapper mapper;
    private final BookRepository bookRepository;

    public BookStockLocationServiceImpl(BookStockLocationRepository repository,
                                        BookRepository bookRepository,
                                        BookStockLocationMapper mapper) {
        super(repository, mapper);
        this.repository = repository;
        this.mapper = mapper;
        this.bookRepository = bookRepository;
    }

    @Override
    protected void setId(BookStockLocation entity, Long id) {
        entity.setId(id);
    }

    @Override
    public BookStockLocationDTO save(BookStockLocationDTO dto) {
        BookStockLocation entity = mapper.toEntity(dto);

        Book book = bookRepository.findBySku(dto.getBookSku())
                .orElseThrow(() -> new RuntimeException("Libro no encontrado con SKU: " + dto.getBookSku()));

        entity.setBook(book);

        BookStockLocation saved = repository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public List<BookStockLocationDTO> saveAll(List<BookStockLocationDTO> dtoList) {
        List<BookStockLocation> entities = dtoList.stream().map(dto -> {
            BookStockLocation entity = mapper.toEntity(dto);
            Book book = bookRepository.findBySku(dto.getBookSku())
                    .orElseThrow(() -> new RuntimeException("Libro no encontrado con SKU: " + dto.getBookSku()));
            entity.setBook(book);
            return entity;
        }).collect(Collectors.toList());

        List<BookStockLocation> savedEntities = repository.saveAll(entities);
        return savedEntities.stream().map(mapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public java.util.Optional<BookStockLocationDTO> update(Long id, BookStockLocationDTO dto) {
        return repository.findById(id).map(existing -> {
            BookStockLocation updated = mapper.toEntity(dto);

            Book book = bookRepository.findBySku(dto.getBookSku())
                    .orElseThrow(() -> new RuntimeException("Libro no encontrado con SKU: " + dto.getBookSku()));

            updated.setBook(book);
            updated.setId(id);

            BookStockLocation saved = repository.save(updated);
            return mapper.toDTO(saved);
        });
    }

    @Override
    public Optional<BookStockLocationDTO> partialUpdate(Long id, Map<String, Object> updates) {
        return repository.findById(id).map(entity -> {

            updates.forEach((key, value) -> {
                if (value == null) return;

                try {
                    Field field = BookStockLocation.class.getDeclaredField(key);
                    field.setAccessible(true);

                    if ("locationType".equals(key)) {
                        field.set(entity, LocationType.valueOf(value.toString()));
                    } else if ("bookCondition".equals(key)) {
                        field.set(entity, BookCondition.valueOf(value.toString()));
                    } else {
                        // Conversión simple por tipo
                        if (field.getType() == Integer.class || field.getType() == int.class) {
                            field.set(entity, Integer.valueOf(value.toString()));
                        } else if (field.getType() == String.class) {
                            field.set(entity, value.toString());
                        } else if (field.getType() == LocalDateTime.class) {
                            field.set(entity, LocalDateTime.parse(value.toString()));
                        } else {
                            field.set(entity, value);
                        }
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // Ignorar si el campo no existe o no es accesible
                }
            });

            repository.save(entity);
            return mapper.toDTO(entity);
        });
    }


    @Override
    public Page<BookStockLocationDTO> searchLocations(BookStockLocationSearchCriteria criteria, Pageable pageable) {
        BookStockLocationSpecification spec = new BookStockLocationSpecification();
        return repository.findAll(spec.build(criteria), pageable).map(mapper::toDTO);
    }
}
