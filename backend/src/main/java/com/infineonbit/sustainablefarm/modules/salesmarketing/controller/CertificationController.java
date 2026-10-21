package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Certification;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.CertificationService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CertificationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {

    private final CertificationService service;
    private final SalesMarketingRequestMapper requestMapper;

    public CertificationController(CertificationService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<Certification> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Certification getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Certification create(@Valid @RequestBody CertificationRequest request) {
        return service.create(requestMapper.toEntity(request, Certification.class));
    }

    @PutMapping("/{id}")
    public Certification update(@PathVariable Integer id, @Valid @RequestBody CertificationRequest request) {
        return service.update(id, requestMapper.toEntity(request, Certification.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
