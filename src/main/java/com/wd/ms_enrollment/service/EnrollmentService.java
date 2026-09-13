package com.wd.ms_enrollment.service;

import com.wd.ms_enrollment.client.EventCategoryClient;
import com.wd.ms_enrollment.client.ModalityClient;
import com.wd.ms_enrollment.repository.EnrollmentRepository;
import com.wd.ms_enrollment.repository.UserEventRoleRepository;
import com.world_dance.wd_lib_common.dto.ApproveEnrollmentRequestDto;
import com.world_dance.wd_lib_common.dto.EnrollmentRequestDto;
import com.world_dance.wd_lib_common.dto.EnrollmentResponseDto;
import com.world_dance.wd_lib_common.dto.EventResponseDto;
import com.world_dance.wd_lib_common.dto.HttpGlobalResponse;
import com.world_dance.wd_lib_common.dto.ModalityResponseDto;
import com.world_dance.wd_lib_common.dto.UserEventRoleResponseDto;
import com.world_dance.wd_lib_common.entity.Enrollment;
import com.world_dance.wd_lib_common.entity.UserEventRole;
import com.world_dance.wd_lib_common.enums.Category;
import com.world_dance.wd_lib_common.enums.EnrollmentStatus;

import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserEventRoleRepository userEventRoleRepository;
    private final EventCategoryClient eventCategoryClient;
    private final ModalityClient modalityClient;

    public EnrollmentResponseDto registerUserToEvent(EnrollmentRequestDto request) {

        boolean isAlreadyEnrolled = enrollmentRepository.existsByUserIdAndEventIdAndModalityId(
                request.getUserId(),
                request.getEventId(),
                request.getModalityId()
        );

        if (isAlreadyEnrolled) {
            throw new IllegalStateException("El usuario ya se encuentra inscrito en esta modalidad para el evento especificado.");
        }

        boolean hasRole = userEventRoleRepository.existsByUserIdAndEventIdAndRoleInEvent(
                request.getUserId(),
                request.getEventId(),
                request.getRoleInEvent()
        );

        if (!hasRole) {
            UserEventRole newRole = UserEventRole.builder()
                    .userId(request.getUserId())
                    .eventId(request.getEventId())
                    .roleInEvent(request.getRoleInEvent())
                    .build();
            userEventRoleRepository.save(newRole);
        }

        Enrollment newEnrollment = Enrollment.builder()
                .userId(request.getUserId())
                .eventId(request.getEventId())
                .modalityId(request.getModalityId())
                .status(EnrollmentStatus.PENDING)
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(newEnrollment);

        return EnrollmentResponseDto.builder()
                .enrollmentId(savedEnrollment.getId())
                .userId(savedEnrollment.getUserId())
                .eventId(savedEnrollment.getEventId())
                .modalityId(savedEnrollment.getModalityId())
                .roleInEvent(request.getRoleInEvent())
                .status(savedEnrollment.getStatus())
                .createdAt(savedEnrollment.getCreatedAt())
                .build();
    }

    /**
     * RF-27: Aprobar inscripción
     * RF-28: Rechazar inscripción con justificación
     */
    public EnrollmentResponseDto approveOrRejectEnrollment(ApproveEnrollmentRequestDto request, Long authenticatedUserId) {

        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la inscripción especificada."));

        HttpGlobalResponse<EventResponseDto> eventResponse = eventCategoryClient.getEventById(enrollment.getEventId());
        EventResponseDto event = eventResponse != null ? eventResponse.getData() : null;

        if (event == null) {
            throw new IllegalArgumentException("El evento asociado a la inscripción no fue encontrado.");
        }

        if (event.getOwnerId() == null || !event.getOwnerId().equals(authenticatedUserId)) {
            throw new SecurityException("Acceso denegado: Solo el creador del evento puede aprobar o rechazar inscripciones.");
        }

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new IllegalStateException("La inscripción ya fue procesada anteriormente y su estado es: " + enrollment.getStatus());
        }

        if (request.getStatus() != EnrollmentStatus.APPROVED && request.getStatus() != EnrollmentStatus.REJECTED) {
            throw new IllegalArgumentException("El nuevo estado debe ser APPROVED o REJECTED.");
        }

        if (request.getStatus() == EnrollmentStatus.REJECTED) {
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw new IllegalArgumentException("Debe indicar una justificación para rechazar la inscripción.");
            }
            enrollment.setReason(request.getReason());
        } else {
            enrollment.setReason(null);
        }

        enrollment.setStatus(request.getStatus());
        Enrollment updatedEnrollment = enrollmentRepository.save(enrollment);

        return EnrollmentResponseDto.builder()
                .enrollmentId(updatedEnrollment.getId())
                .userId(updatedEnrollment.getUserId())
                .eventId(updatedEnrollment.getEventId())
                .modalityId(updatedEnrollment.getModalityId())
                .status(updatedEnrollment.getStatus())
                .createdAt(updatedEnrollment.getCreatedAt())
                .build();
    }

    /**
     * RF-29: Consultar inscritos por categoría (ORGANIZADOR/ADMIN).
     */
    public List<EnrollmentResponseDto> getEnrollmentsByCategory(Category category, Long authenticatedUserId) {

        HttpGlobalResponse<List<ModalityResponseDto>> modalitiesResponse =
                modalityClient.getModalitiesByCategory(category);

        List<ModalityResponseDto> modalities = modalitiesResponse != null ? modalitiesResponse.getData() : null;

        if (modalities == null || modalities.isEmpty()) {
            return List.of();
        }

        Set<Long> uniqueEventIds = modalities.stream()
                .map(ModalityResponseDto::getEventId)
                .collect(Collectors.toSet());

        Set<Long> ownedEventIds = new HashSet<>();
        for (Long eventId : uniqueEventIds) {
            HttpGlobalResponse<EventResponseDto> eventResponse = eventCategoryClient.getEventById(eventId);
            EventResponseDto event = eventResponse != null ? eventResponse.getData() : null;
            // BYPASS TEMPORAL: Permitir que los jurados también vean los participantes, no solo el organizador.
            if (event != null) {
                ownedEventIds.add(eventId);
            }
        }

        if (ownedEventIds.isEmpty()) {
            return List.of();
        }

        List<Long> allowedModalityIds = modalities.stream()
                .filter(m -> ownedEventIds.contains(m.getEventId()))
                .map(ModalityResponseDto::getId)
                .toList();

        List<Enrollment> enrollments = enrollmentRepository.findByModalityIdIn(allowedModalityIds);

        return enrollments.stream()
                .map(this::toResponseDto)
                .toList();
    }

    /**
     * RF-30: Consultar mis inscripciones y sus estados (PARTICIPANTE).
     */
    public List<EnrollmentResponseDto> getMyEnrollments(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        return enrollments.stream()
                .map(this::toResponseDto)
                .toList();
    }

    private EnrollmentResponseDto toResponseDto(Enrollment e) {
        return EnrollmentResponseDto.builder()
                .enrollmentId(e.getId())
                .userId(e.getUserId())
                .eventId(e.getEventId())
                .modalityId(e.getModalityId())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }


    public UserEventRoleResponseDto getUserEventRole(Long userId, Long eventId) {
        UserEventRole userEventRole = userEventRoleRepository.findByUserIdAndEventId(userId, eventId)
            .orElseThrow(() -> new IllegalArgumentException(
                    "El usuario no tiene un rol asignado en este evento."));

        return UserEventRoleResponseDto.builder()
            .id(userEventRole.getId())
            .userId(userEventRole.getUserId())
            .eventId(userEventRole.getEventId())
            .roleInEvent(userEventRole.getRoleInEvent())
            .build();
    }
}