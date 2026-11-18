package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.plants.api.CatalogVariety;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Product;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The products. Their variety is a key of the plants variety catalogue: a name
 * sent ({@code "Kent"}, {@code "kent"}, {@code "Amélie"}) is resolved to its key,
 * and a product read carries the catalogue name of its variety.
 */
@Service
public class ProductService {

    private final ProductRepository repository;
    private final VarietyCatalog varietyCatalog;

    public ProductService(ProductRepository repository, VarietyCatalog varietyCatalog) {
        this.repository = repository;
        this.varietyCatalog = varietyCatalog;
    }

    public List<Product> getAll() {
        List<Product> products = repository.findAll();
        Map<String, String> names = varietyCatalog.listVarieties().stream()
                .collect(Collectors.toMap(CatalogVariety::key, CatalogVariety::name, (a, b) -> a));
        products.forEach(product -> fillVarietyName(product, names::get));
        return products;
    }

    public Product getById(Integer id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return withVarietyName(product);
    }

    /**
     * Creates a product.
     *
     * @throws BadRequestException if the variety is not in the catalogue
     */
    public Product create(Product item) {
        item.setVarietyKey(varietyKeyOf(item.getVariety()));
        return withVarietyName(repository.save(item));
    }

    /**
     * Copies every editable field from updatedData onto the existing
     * row, then saves it. The id and created_at are left untouched.
     *
     * @throws BadRequestException if the variety is not in the catalogue
     */
    public Product update(Integer id, Product updatedData) {
        Product existing = getById(id);

        existing.setName(updatedData.getName());
        existing.setVarietyKey(varietyKeyOf(updatedData.getVariety()));
        existing.setUnit(updatedData.getUnit());

        return withVarietyName(repository.save(existing));
    }

    public void delete(Integer id) {
        repository.deleteById(id);
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

    private Product withVarietyName(Product product) {
        fillVarietyName(product, key -> varietyCatalog.findByKey(key).map(CatalogVariety::name).orElse(null));
        return product;
    }

    private static void fillVarietyName(Product product, Function<String, String> nameOfKey) {
        product.setVariety(product.getVarietyKey() == null ? null : nameOfKey.apply(product.getVarietyKey()));
    }
}
