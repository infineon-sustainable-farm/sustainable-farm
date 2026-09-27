package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Order;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public List<Order> getAll() {
        return repository.findAll();
    }

    public Order getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    public Order create(Order item) {
        return repository.save(item);
    }

    public Order update(Integer id, Order updatedData) {
        Order existing = getById(id);

        existing.setCustomerId(updatedData.getCustomerId());
        existing.setSalesChannelId(updatedData.getSalesChannelId());
        existing.setOrderDate(updatedData.getOrderDate());
        existing.setTotalVolumeKg(updatedData.getTotalVolumeKg());
        existing.setTotalValueEur(updatedData.getTotalValueEur());
        existing.setStatus(updatedData.getStatus());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
