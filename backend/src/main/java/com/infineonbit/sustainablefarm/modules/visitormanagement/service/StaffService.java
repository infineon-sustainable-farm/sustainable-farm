package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.StaffRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.StaffResponse;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Business logic for farm staff / guides (A5).
 */
public interface StaffService {

    List<StaffResponse> list();

    Page<StaffResponse> list(int page, int size);

    StaffResponse get(Long id);

    StaffResponse create(StaffRequest request);

    StaffResponse update(Long id, StaffRequest request);

    void deactivate(Long id);
}