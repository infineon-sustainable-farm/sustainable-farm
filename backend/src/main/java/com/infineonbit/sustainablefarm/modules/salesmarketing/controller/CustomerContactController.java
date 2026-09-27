package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerContact;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.CustomerContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-contacts")
@CrossOrigin(origins = "http://localhost:3000")
public class CustomerContactController {

    private final CustomerContactService service;

    public CustomerContactController(CustomerContactService service) {
        this.service = service;
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
    public CustomerContact create(@Valid @RequestBody CustomerContact item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public CustomerContact update(@PathVariable Integer id, @Valid @RequestBody CustomerContact item) {
        return service.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
