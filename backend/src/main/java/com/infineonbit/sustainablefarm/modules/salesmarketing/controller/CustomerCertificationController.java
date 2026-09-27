package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerCertification;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerCertificationId;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CustomerCertificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/certifications")
@CrossOrigin(origins = "http://localhost:3000")
public class CustomerCertificationController {

    private final CustomerCertificationRepository repository;

    public CustomerCertificationController(CustomerCertificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CustomerCertification> getForCustomer(
            @PathVariable Integer customerId) {
        return repository.findByCustomerId(customerId);
    }

    @PostMapping("/{certificationId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerCertification link(
            @PathVariable Integer customerId,
            @PathVariable Integer certificationId) {

        return repository.save(
                new CustomerCertification(customerId, certificationId)
        );
    }

    @DeleteMapping("/{certificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(
            @PathVariable Integer customerId,
            @PathVariable Integer certificationId) {

        repository.deleteById(
                new CustomerCertificationId(customerId, certificationId)
        );
    }
}