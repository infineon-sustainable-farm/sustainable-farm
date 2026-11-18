package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.CustomerCertificationResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerCertification;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CertificationRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CustomerCertificationRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The certifications held by the customers. Linking twice keeps one link, unlinking a link that
 * does not exist does nothing; an unknown customer or certification is refused by its foreign
 * key (409).
 */
@Service
@Transactional
public class CustomerCertificationService {

    private final CustomerCertificationRepository repository;
    private final CustomerRepository customers;
    private final CertificationRepository certifications;

    public CustomerCertificationService(CustomerCertificationRepository repository, CustomerRepository customers,
                                        CertificationRepository certifications) {
        this.repository = repository;
        this.customers = customers;
        this.certifications = certifications;
    }

    @Transactional(readOnly = true)
    public List<CustomerCertificationResponse> getForCustomer(Long customerId) {
        return repository.findByCustomerId(customerId).stream().map(CustomerCertificationResponse::of).toList();
    }

    public CustomerCertificationResponse link(Long customerId, Long certificationId) {
        CustomerCertification link = repository.findByCustomerIdAndCertificationId(customerId, certificationId)
                .orElseGet(() -> repository.save(new CustomerCertification(
                        customers.getReferenceById(customerId), certifications.getReferenceById(certificationId))));
        return CustomerCertificationResponse.of(link);
    }

    public void unlink(Long customerId, Long certificationId) {
        repository.findByCustomerIdAndCertificationId(customerId, certificationId).ifPresent(repository::delete);
    }
}
