package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

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

    public Customer createCustomer(Customer customer) {
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Integer id, Customer updatedData) {
        Customer existing = getCustomerById(id);

        existing.setCompanyName(updatedData.getCompanyName());
        existing.setCustomerType(updatedData.getCustomerType());
        existing.setCountry(updatedData.getCountry());
        existing.setRegion(updatedData.getRegion());
        existing.setCity(updatedData.getCity());
        existing.setStatus(updatedData.getStatus());
        existing.setPaymentTerms(updatedData.getPaymentTerms());
        existing.setUpdatedAt(LocalDateTime.now());

        return customerRepository.save(existing);
    }

    public void deleteCustomer(Integer id) {
        customerRepository.deleteById(id);
    }
}
