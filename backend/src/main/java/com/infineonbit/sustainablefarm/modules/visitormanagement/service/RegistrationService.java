package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BriefingDeliverRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.BriefingResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.RegistrationResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorRequest;
import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.VisitorResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for the visitor database and registration flow
 * (Registration sub-module).
 */
public interface RegistrationService {

    VisitorResponse createVisitor(VisitorRequest request);

    VisitorResponse updateVisitor(Long id, VisitorRequest request);

    VisitorResponse getVisitor(Long id);

    List<VisitorResponse> listVisitors();

    RegistrationResponse register(RegistrationRequest request);

    RegistrationResponse approve(Long registrationId);

    RegistrationResponse reject(Long registrationId);

    RegistrationResponse checkIn(Long registrationId);

    RegistrationResponse cancel(Long registrationId);

    RegistrationResponse getRegistration(Long id);

    List<RegistrationResponse> getRegistrationsBySlot(Long timeSlotId);

    List<RegistrationResponse> getRegistrationsByDate(LocalDate date);

    BriefingResponse getBriefingForRegistration(Long registrationId);

    BriefingResponse deliverBriefing(Long registrationId, BriefingDeliverRequest request);
}
