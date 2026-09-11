package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TourStopRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.TourStopResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.WorkshopRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.WorkshopResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.WorkshopStatus;

import java.util.List;

/**
 * Business logic for the educational program: the standard guided-tour stops
 * (welcoming, orchard, irrigation, solar, processing, wrap-up) and the
 * reusable workshop / tour templates offered to visitor groups.
 */
public interface EducationalProgramService {

    List<TourStopResponse> listStops();

    TourStopResponse createStop(TourStopRequest request);

    TourStopResponse updateStop(Long id, TourStopRequest request);

    void deactivateStop(Long id);

    List<WorkshopResponse> listWorkshops(WorkshopStatus status);

    WorkshopResponse createWorkshop(WorkshopRequest request);

    WorkshopResponse updateWorkshop(Long id, WorkshopRequest request);

    WorkshopResponse publishWorkshop(Long id);

    WorkshopResponse deactivateWorkshop(Long id);
}