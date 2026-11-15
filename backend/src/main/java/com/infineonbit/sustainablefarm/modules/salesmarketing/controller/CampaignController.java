package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Campaign;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.CampaignService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CampaignRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService service;
    private final SalesMarketingRequestMapper requestMapper;

    public CampaignController(CampaignService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<Campaign> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Campaign getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Campaign create(@Valid @RequestBody CampaignRequest request) {
        return service.create(requestMapper.toEntity(request, Campaign.class));
    }

    @PutMapping("/{id}")
    public Campaign update(
            @PathVariable Integer id,
            @Valid @RequestBody CampaignRequest request) {
        return service.update(id, requestMapper.toEntity(request, Campaign.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}