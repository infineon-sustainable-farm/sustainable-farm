package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.plants.api.CatalogVariety;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ProductRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.ProductResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Product;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The products. Their variety is a key of the plants variety catalogue: a name
 * sent ({@code "Kent"}, {@code "kent"}, {@code "Amélie"}) is resolved to its key,
 * and a product read carries the catalogue name of its variety.
 */
@Service
@Transactional
public class ProductService {

    private final ProductRepository repository;
    private final VarietyCatalog varietyCatalog;
    private final SalesMarketingRequestMapper mapper;

    public ProductService(ProductRepository repository, VarietyCatalog varietyCatalog,
                          SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.varietyCatalog = varietyCatalog;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        Map<String, String> names = varietyCatalog.listVarieties().stream()
                .collect(Collectors.toMap(CatalogVariety::key, CatalogVariety::name, (a, b) -> a));
        return repository.findAll().stream()
                .map(product -> ProductResponse.of(product,
                        product.getVarietyKey() == null ? null : names.get(product.getVarietyKey())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return withVarietyName(find(id));
    }

    /**
     * Creates a product.
     *
     * @throws BadRequestException if the variety is not in the catalogue
     */
    public ProductResponse create(ProductRequest request) {
        Product product = mapper.apply(request, new Product());
        product.setVarietyKey(varietyKeyOf(request.variety()));
        return withVarietyName(repository.save(product));
    }

    /**
     * Replaces the name, the variety and the unit of a product.
     *
     * @throws BadRequestException if the variety is not in the catalogue
     */
    public ProductResponse update(Long id, ProductRequest request) {
        Product existing = mapper.apply(request, find(id));
        existing.setVarietyKey(varietyKeyOf(request.variety()));
        return withVarietyName(repository.save(existing));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private Product find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    /** The catalogue key of a variety name, {@code null} for no variety. */
    private String varietyKeyOf(String varietyName) {
        if (varietyName == null || varietyName.isBlank()) {
            return null;
        }
        return varietyCatalog.resolve(varietyName)
                .map(CatalogVariety::key)
                .orElseThrow(() -> new BadRequestException("Unknown variety: " + varietyName));
    }

    private ProductResponse withVarietyName(Product product) {
        String name = product.getVarietyKey() == null ? null
                : varietyCatalog.findByKey(product.getVarietyKey()).map(CatalogVariety::name).orElse(null);
        return ProductResponse.of(product, name);
    }
}
