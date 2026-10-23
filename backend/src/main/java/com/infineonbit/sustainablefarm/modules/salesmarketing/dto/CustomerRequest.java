package com.infineonbit.sustainablefarm.modules.salesmarketing.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank(message = "Company name is required") String companyName,
        String customerCode,
        @NotBlank(message = "Customer type is required") String customerType,
        String country,
        String region,
        String city,
        @NotBlank(message = "Status is required") String status,
        String paymentTerms
) {}
