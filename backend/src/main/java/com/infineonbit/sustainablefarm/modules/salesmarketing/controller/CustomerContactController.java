package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerContact;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.CustomerContactService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CustomerContactRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-contacts")
public class CustomerContactController {

    private final CustomerContactService service;
    private final SalesMarketingRequestMapper requestMapper;

    public CustomerContactController(CustomerContactService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<CustomerContact> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CustomerContact getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerContact create(@Valid @RequestBody CustomerContactRequest request) {
        return service.create(requestMapper.toEntity(request, CustomerContact.class));
    }

    @PutMapping("/{id}")
    public CustomerContact update(@PathVariable Integer id, @Valid @RequestBody CustomerContactRequest request) {
        return service.update(id, requestMapper.toEntity(request, CustomerContact.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
