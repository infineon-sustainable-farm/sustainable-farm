package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.CustomerCertificationResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.CustomerCertificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/certifications")
public class CustomerCertificationController {

    private final CustomerCertificationService service;

    public CustomerCertificationController(CustomerCertificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerCertificationResponse> getForCustomer(@PathVariable Long customerId) {
        return service.getForCustomer(customerId);
    }

    @PostMapping("/{certificationId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerCertificationResponse link(@PathVariable Long customerId, @PathVariable Long certificationId) {
        return service.link(customerId, certificationId);
    }

    @DeleteMapping("/{certificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable Long customerId, @PathVariable Long certificationId) {
        service.unlink(customerId, certificationId);
    }
}
