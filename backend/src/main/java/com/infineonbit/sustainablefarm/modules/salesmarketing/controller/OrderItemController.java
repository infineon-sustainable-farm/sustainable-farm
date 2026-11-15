package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.OrderItem;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.OrderItemService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.OrderItemRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

    private final OrderItemService service;
    private final SalesMarketingRequestMapper requestMapper;

    public OrderItemController(OrderItemService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<OrderItem> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public OrderItem getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItem create(@Valid @RequestBody OrderItemRequest request) {
        return service.create(requestMapper.toEntity(request, OrderItem.class));
    }

    @PutMapping("/{id}")
    public OrderItem update(@PathVariable Integer id, @Valid @RequestBody OrderItemRequest request) {
        return service.update(id, requestMapper.toEntity(request, OrderItem.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
