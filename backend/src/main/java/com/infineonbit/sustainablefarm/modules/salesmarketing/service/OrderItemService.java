package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.OrderItemRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.OrderItemResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.OrderItem;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderItemService {

    private final OrderItemRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public OrderItemService(OrderItemRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<OrderItemResponse> getAll() {
        return repository.findAll().stream().map(OrderItemResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public OrderItemResponse getById(Long id) {
        return OrderItemResponse.of(find(id));
    }

    public OrderItemResponse create(OrderItemRequest request) {
        return OrderItemResponse.of(repository.save(mapper.apply(request, new OrderItem())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public OrderItemResponse update(Long id, OrderItemRequest request) {
        return OrderItemResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * @throws ResourceNotFoundException if no row has this id
     */
    private OrderItem find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found with id: " + id));
    }
}
