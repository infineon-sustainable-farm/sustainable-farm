package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Customer;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.CustomerRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/customers")

public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<Customer> getCustomers(
            @RequestParam(required = false) String status) {

        if (status != null) {
            return customerService.getCustomersByStatus(status);
        }

        return customerService.getAllCustomers();
    }

    @GetMapping("/{id}")
    public Customer getCustomer(@PathVariable Integer id) {
        return customerService.getCustomerById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer createCustomer(
        @Valid @RequestBody CustomerRequest request) {

    return customerService.createCustomer(request);
}

    @PutMapping("/{id}")
   public Customer updateCustomer(
        @PathVariable Integer id,
        @Valid @RequestBody CustomerRequest request) {

    return customerService.updateCustomer(id, request);
}

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Integer id) {
        customerService.deleteCustomer(id);
    }
}
