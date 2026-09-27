package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerContact;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CustomerContactRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerContactService {

    private final CustomerContactRepository repository;

    public CustomerContactService(CustomerContactRepository repository) {
        this.repository = repository;
    }

    public List<CustomerContact> getAll() {
        return repository.findAll();
    }

    public CustomerContact getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerContact not found with id: " + id));
    }

    public CustomerContact create(CustomerContact item) {
        return repository.save(item);
    }

    /**
     * Copies every editable field from updatedData onto the existing
     * row, then saves it.
     */
    public CustomerContact update(Integer id, CustomerContact updatedData) {
        CustomerContact existing = getById(id);
        existing.setCustomerId(updatedData.getCustomerId());
        existing.setFullName(updatedData.getFullName());
        existing.setEmail(updatedData.getEmail());
        existing.setPhone(updatedData.getPhone());
        existing.setRole(updatedData.getRole());
        existing.setIsPrimary(updatedData.getIsPrimary());
        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
