package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.DemandReportRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.DemandReportResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandReport;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.DemandReportRepository;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DemandReportService {

    private final DemandReportRepository repository;
    private final SalesMarketingRequestMapper mapper;
    private final PersonDirectory personDirectory;

    public DemandReportService(DemandReportRepository repository, SalesMarketingRequestMapper mapper,
                               PersonDirectory personDirectory) {
        this.repository = repository;
        this.mapper = mapper;
        this.personDirectory = personDirectory;
    }

    @Transactional(readOnly = true)
    public List<DemandReportResponse> getAll() {
        List<DemandReport> reports = repository.findAll();
        Map<Long, String> names = personDirectory.namesOf(reports.stream().map(DemandReport::getGeneratedByPersonId).toList());
        return reports.stream().map(r -> DemandReportResponse.of(r, names.get(r.getGeneratedByPersonId()))).toList();
    }

    @Transactional(readOnly = true)
    public DemandReportResponse getById(Long id) {
        return response(find(id));
    }

    public DemandReportResponse create(DemandReportRequest request) {
        return response(repository.save(apply(request, new DemandReport())));
    }

    /** Replaces every field of the request; the id and the creation time stay. */
    public DemandReportResponse update(Long id, DemandReportRequest request) {
        return response(repository.save(apply(request, find(id))));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /** The request fields, and the person who generated the report checked against the people directory. */
    private DemandReport apply(DemandReportRequest request, DemandReport report) {
        mapper.apply(request, report);
        report.setGeneratedByPersonId(personDirectory.requireExisting(request.generatedByPersonId()));
        return report;
    }

    private DemandReportResponse response(DemandReport report) {
        return DemandReportResponse.of(report, personDirectory.nameOf(report.getGeneratedByPersonId()));
    }

    /**
     * @throws ResourceNotFoundException if no row has this id
     */
    private DemandReport find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandReport not found with id: " + id));
    }
}
