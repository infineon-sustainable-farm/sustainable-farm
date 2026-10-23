package com.infineonbit.sustainablefarm.modules.salesmarketing.repository;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    List<Customer> findByStatus(String status);

    List<Customer> findByCountry(String country);
}
