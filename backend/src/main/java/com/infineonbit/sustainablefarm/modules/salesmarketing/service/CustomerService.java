package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.CustomerRequest;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Customer;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    public List<Customer> getCustomersByStatus(String status) {
        return customerRepository.findByStatus(status);
    }

    public Customer createCustomer(CustomerRequest request) {
    Customer customer = new Customer();
    customer.setCompanyName(request.companyName());
    customer.setCustomerCode(request.customerCode());
    customer.setCustomerType(request.customerType());
    customer.setCountry(request.country());
    customer.setRegion(request.region());
    customer.setCity(request.city());
    customer.setStatus(request.status());
    customer.setPaymentTerms(request.paymentTerms());

    LocalDateTime now = LocalDateTime.now();
    customer.setCreatedAt(now);
    customer.setUpdatedAt(now);

    return customerRepository.save(customer);
}

   public Customer updateCustomer(Integer id, CustomerRequest request) {
    Customer existing = getCustomerById(id);

    existing.setCompanyName(request.companyName());
    existing.setCustomerCode(request.customerCode());
    existing.setCustomerType(request.customerType());
    existing.setCountry(request.country());
    existing.setRegion(request.region());
    existing.setCity(request.city());
    existing.setStatus(request.status());
    existing.setPaymentTerms(request.paymentTerms());
    existing.setUpdatedAt(LocalDateTime.now());

    return customerRepository.save(existing);
}

    public void deleteCustomer(Integer id) {
        customerRepository.deleteById(id);
    }
}
