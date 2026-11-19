package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.OrderRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.OrderResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Order;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository repository;
    private final SalesMarketingRequestMapper mapper;

    public OrderService(OrderRepository repository, SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAll() {
        return repository.findAll().stream().map(OrderResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return OrderResponse.of(find(id));
    }

    public OrderResponse create(OrderRequest request) {
        return OrderResponse.of(repository.save(mapper.apply(request, new Order())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public OrderResponse update(Long id, OrderRequest request) {
        return OrderResponse.of(repository.save(mapper.apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * @throws ResourceNotFoundException if no row has this id
     */
    private Order find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }
}
