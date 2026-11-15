package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Certification;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CertificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CertificationService {

    private final CertificationRepository repository;

    public CertificationService(CertificationRepository repository) {
        this.repository = repository;
    }

    public List<Certification> getAll() {
        return repository.findAll();
    }

    public Certification getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certification not found with id: " + id));
    }

    public Certification create(Certification item) {
        return repository.save(item);
    }

    /**
     * Copies every editable field from updatedData onto the existing
     * row, then saves it.
     */
    public Certification update(Integer id, Certification updatedData) {
        Certification existing = getById(id);
        existing.setName(updatedData.getName());
        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
