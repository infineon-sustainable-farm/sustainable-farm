package com.infineonbit.sustainablefarm.modules.salesmarketing.repository;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerCertification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerCertificationRepository extends JpaRepository<CustomerCertification, Long> {

    List<CustomerCertification> findByCustomerId(Long customerId);

    Optional<CustomerCertification> findByCustomerIdAndCertificationId(Long customerId, Long certificationId);
}
