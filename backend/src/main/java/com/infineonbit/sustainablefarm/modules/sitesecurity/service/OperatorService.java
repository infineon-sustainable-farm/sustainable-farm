package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.CreateOperatorRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.OperatorDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages who may sign in. The site must always keep at least one active admin. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperatorService {

    private static final Sort BY_EMAIL = Sort.by("email");

    private final OperatorRepository operatorRepository;

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public List<OperatorDto> getOperators() {
        return operatorRepository.findAll(BY_EMAIL).stream().map(OperatorService::toDto).toList();
    }

    @Transactional
    public OperatorDto addOperator(CreateOperatorRequestDto request, String actor) {
        String email = normalizeEmail(request.getEmail());
        if (operatorRepository.existsByEmail(email)) {
            throw duplicateOperator(email);
        }
        OperatorEntity operator = OperatorEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .role(parseRole(request.getRole()))
                .active(true)
                .createdBy(actor)
                .createdAt(Instant.now())
                .build();
        try {
            // Flush here so a concurrent duplicate fails inside this method, as a 409.
            return toDto(operatorRepository.saveAndFlush(operator));
        } catch (DataIntegrityViolationException e) {
            throw duplicateOperator(email);
        }
    }

    @Transactional
    public OperatorDto changeRole(String id, String role) {
        SiteSecurityRole newRole = parseRole(role);
        List<OperatorEntity> activeAdmins = operatorRepository.lockActiveByRole(SiteSecurityRole.ADMIN);
        OperatorEntity operator = findOperator(id);
        if (newRole != SiteSecurityRole.ADMIN) {
            rejectIfLastAdmin(operator, activeAdmins);
        }
        operator.setRole(newRole);
        return toDto(operatorRepository.save(operator));
    }

    /** Idempotent: disabling an operator who is already disabled changes nothing. */
    @Transactional
    public OperatorDto disableOperator(String id, String actor) {
        List<OperatorEntity> activeAdmins = operatorRepository.lockActiveByRole(SiteSecurityRole.ADMIN);
        OperatorEntity operator = findOperator(id);
        if (!operator.isActive()) {
            return toDto(operator);
        }
        rejectIfLastAdmin(operator, activeAdmins);
        operator.setActive(false);
        operator.setDisabledBy(actor);
        operator.setDisabledAt(Instant.now());
        return toDto(operatorRepository.save(operator));
    }

    /** Idempotent: enabling an active operator changes nothing. */
    @Transactional
    public OperatorDto enableOperator(String id) {
        OperatorEntity operator = findOperator(id);
        if (operator.isActive()) {
            return toDto(operator);
        }
        operator.setActive(true);
        operator.setDisabledBy(null);
        operator.setDisabledAt(null);
        return toDto(operatorRepository.save(operator));
    }

    private OperatorEntity findOperator(String id) {
        return operatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No operator with id '" + id + "'."));
    }

    private static void rejectIfLastAdmin(OperatorEntity operator, List<OperatorEntity> activeAdmins) {
        boolean isActiveAdmin = operator.isActive() && operator.getRole() == SiteSecurityRole.ADMIN;
        if (isActiveAdmin && activeAdmins.size() == 1) {
            throw new BusinessRuleException("The last active admin cannot be disabled or demoted.");
        }
    }

    private static SiteSecurityRole parseRole(String role) {
        return SiteSecurityRole.valueOf(role.toUpperCase(Locale.ROOT));
    }

    private static ConflictException duplicateOperator(String email) {
        return new ConflictException("An operator with the email '" + email + "' already exists.");
    }

    private static OperatorDto toDto(OperatorEntity operator) {
        return OperatorDto.builder()
                .id(operator.getId())
                .email(operator.getEmail())
                .role(operator.getRole().name().toLowerCase(Locale.ROOT))
                .active(operator.isActive())
                .createdBy(operator.getCreatedBy())
                .createdAt(operator.getCreatedAt())
                .disabledBy(operator.getDisabledBy())
                .disabledAt(operator.getDisabledAt())
                .lastLoginAt(operator.getLastLoginAt())
                .build();
    }
}
