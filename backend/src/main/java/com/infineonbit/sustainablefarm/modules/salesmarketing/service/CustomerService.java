package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.CustomerRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.CustomerResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Customer;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerStatus;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.StoredValue;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * The customers. Deleting a customer deletes its contacts and certification links; a customer
 * with orders cannot be deleted (409).
 */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SalesMarketingRequestMapper mapper;

    public CustomerService(CustomerRepository customerRepository, SalesMarketingRequestMapper mapper) {
        this.customerRepository = customerRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream().map(CustomerResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return CustomerResponse.of(find(id));
    }

    /** The customers of a status; none for a value that is not a status. */
    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByStatus(String status) {
        return StoredValue.find(CustomerStatus.class, status)
                .map(value -> customerRepository.findByStatus(value).stream().map(CustomerResponse::of).toList())
                .orElse(List.of());
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        return CustomerResponse.of(customerRepository.save(mapper.apply(request, new Customer())));
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer existing = mapper.apply(request, find(id));
        // The update time moves on every update, even one that changes nothing, as before.
        existing.setUpdatedAt(Instant.now());
        return CustomerResponse.of(customerRepository.save(existing));
    }

    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    private Customer find(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }
}
