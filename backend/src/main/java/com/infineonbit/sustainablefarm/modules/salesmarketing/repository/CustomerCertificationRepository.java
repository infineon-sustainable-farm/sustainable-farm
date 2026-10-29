package com.infineonbit.sustainablefarm.modules.salesmarketing.repository;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerCertification;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CustomerCertificationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerCertificationRepository
        extends JpaRepository<CustomerCertification, CustomerCertificationId> {

    List<CustomerCertification> findByCustomerId(Integer customerId);

    List<CustomerCertification> findByCertificationId(Integer certificationId);
}