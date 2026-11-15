package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.AlertRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Alert;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("salesMarketingAlertService")
public class AlertService {


private final AlertRepository repository;

public AlertService(AlertRepository repository) {
    this.repository = repository;
}

public List<Alert> getAll() {
    return repository.findAll();
}

public Alert getById(Integer id) {
    return repository.findById(id)
            .orElseThrow(() ->
                    new ResourceNotFoundException("Alert not found with id: " + id));
}

public Alert create(AlertRequest request) {
    Alert item = new Alert();
    item.setAlertCode(request.alertCode());
    item.setAlertType(request.alertType());
    item.setSeverity(request.severity());
    item.setMessage(request.message());
    item.setRegion(request.region());
    item.setOrigin(request.origin());
    item.setRelatedTable(request.relatedTable());
    item.setRelatedId(request.relatedId());
    item.setStatus(request.status());
    item.setResolvedAt(request.resolvedAt());

    return repository.save(item);
}

public Alert update(Integer id, AlertRequest request) {
    Alert existing = getById(id);

    existing.setAlertCode(request.alertCode());
    existing.setAlertType(request.alertType());
    existing.setSeverity(request.severity());
    existing.setMessage(request.message());
    existing.setRegion(request.region());
    existing.setOrigin(request.origin());
    existing.setRelatedTable(request.relatedTable());
    existing.setRelatedId(request.relatedId());
    existing.setStatus(request.status());
    existing.setResolvedAt(request.resolvedAt());

    return repository.save(existing);
}

public void delete(Integer id) {
    repository.deleteById(id);
}

}
