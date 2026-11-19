package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CustomerContactRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.CustomerContactResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.CustomerContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sales-marketing/customer-contacts")
public class CustomerContactController {

    private final CustomerContactService service;

    public CustomerContactController(CustomerContactService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerContactResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CustomerContactResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerContactResponse create(@Valid @RequestBody CustomerContactRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CustomerContactResponse update(@PathVariable Long id, @Valid @RequestBody CustomerContactRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
