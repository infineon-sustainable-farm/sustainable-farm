package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.OrderItem;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.OrderItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderItemService {

    private final OrderItemRepository repository;

    public OrderItemService(OrderItemRepository repository) {
        this.repository = repository;
    }

    public List<OrderItem> getAll() {
        return repository.findAll();
    }

    public OrderItem getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found with id: " + id));
    }

    public OrderItem create(OrderItem item) {
        return repository.save(item);
    }

    /**
     * Copies every editable field from updatedData onto the existing
     * row, then saves it.
     */
    public OrderItem update(Integer id, OrderItem updatedData) {
        OrderItem existing = getById(id);
        existing.setOrderId(updatedData.getOrderId());
        existing.setProductId(updatedData.getProductId());
        existing.setQuantityKg(updatedData.getQuantityKg());
        existing.setUnitPriceEur(updatedData.getUnitPriceEur());
        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
