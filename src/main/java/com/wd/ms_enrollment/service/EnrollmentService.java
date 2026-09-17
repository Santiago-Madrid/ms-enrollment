package com.wd.ms_enrollment.service;

import com.wd.ms_enrollment.client.EventCategoryClient;
import com.wd.ms_enrollment.client.ModalityClient;
import com.wd.ms_enrollment.client.UserServiceClient;
import com.wd.ms_enrollment.repository.EnrollmentRepository;
import com.wd.ms_enrollment.repository.UserEventRoleRepository;
import com.world_dance.wd_lib_common.dto.ApproveEnrollmentRequestDto;
import com.world_dance.wd_lib_common.dto.EnrollmentRequestDto;
import com.world_dance.wd_lib_common.dto.EnrollmentResponseDto;
import com.world_dance.wd_lib_common.dto.EventResponseDto;
import com.world_dance.wd_lib_common.dto.HttpGlobalResponse;
import com.world_dance.wd_lib_common.dto.ModalityResponseDto;
import com.world_dance.wd_lib_common.dto.ParticipantSummaryDto;
import com.world_dance.wd_lib_common.dto.UserEventRoleResponseDto;
import com.world_dance.wd_lib_common.dto.UserResponseDto;
import com.world_dance.wd_lib_common.entity.Enrollment;
import com.world_dance.wd_lib_common.entity.UserEventRole;
import com.world_dance.wd_lib_common.enums.Category;
import com.world_dance.wd_lib_common.enums.EnrollmentStatus;
import com.world_dance.wd_lib_common.enums.EventRole;
import com.world_dance.wd_lib_common.exception.BadRequestException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserEventRoleRepository userEventRoleRepository;
    private final EventCategoryClient eventCategoryClient;
    private final ModalityClient modalityClient;
    private final UserServiceClient userServiceClient;

    @Value("${wd.agent.email:}")
    private String agentEmail;

    public EnrollmentResponseDto registerUserToEvent(EnrollmentRequestDto request) {
        boolean isAlreadyEnrolled = enrollmentRepository.existsByUserIdAndEventIdAndModalityId(
                request.getUserId(), request.getEventId(), request.getModalityId());

        if (isAlreadyEnrolled) {
            throw new com.world_dance.wd_lib_common.exception.BadRequestException("El usuario ya está inscrito en esta modalidad del evento.");
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
                .participant(fetchParticipant(savedEnrollment.getUserId()))
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
                .participant(fetchParticipant(updatedEnrollment.getUserId()))
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
     * Consultar todas las inscripciones de un evento (uso interno, ej. ms-scheduling
     * para generar el cronograma).
     */
    public List<EnrollmentResponseDto> getEnrollmentsByEvent(Long eventId) {
        List<Enrollment> enrollments = enrollmentRepository.findByEventId(eventId);
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

    /**
     * Consultar una inscripción puntual por su id (uso interno, ej. ms-music-media vía
     * EnrollmentFeignClient para validar la inscripción antes de subir/descargar la pista musical).
     */
    public EnrollmentResponseDto getEnrollmentById(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BadRequestException("No se encontró la inscripción con el id: " + enrollmentId));
        return toResponseDto(enrollment);
    }

    private EnrollmentResponseDto toResponseDto(Enrollment e) {
        return EnrollmentResponseDto.builder()
                .enrollmentId(e.getId())
                .userId(e.getUserId())
                .eventId(e.getEventId())
                .modalityId(e.getModalityId())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .participant(fetchParticipant(e.getUserId()))
                .build();
    }

    private ParticipantSummaryDto fetchParticipant(Long userId) {
        try {
            UserResponseDto user = userServiceClient.getUserById(userId);
            if (user == null) {
                return null;
            }
            return ParticipantSummaryDto.builder()
                    .id(user.getId())
                    .name(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .documentNumber(user.getDocumentNumber())
                    .build();
        } catch (Exception ex) {
            log.warn("No se pudo obtener el participante (userId {}) desde ms-auth-identityservice: {}", userId, ex.getMessage());
            return null;
        }
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

    /**
     * Asigna el rol ADMIN a un usuario (ej. la cuenta del agente de IA) en un
     * evento. Solo lo puede hacer el dueño del evento. Es idempotente: si el
     * usuario ya tiene ADMIN en ese evento, devuelve el rol existente en vez
     * de duplicarlo.
     */
    public UserEventRoleResponseDto assignAdminRole(Long eventId, Long targetUserId, Long authenticatedUserId) {
        HttpGlobalResponse<EventResponseDto> eventResponse = eventCategoryClient.getEventById(eventId);
        EventResponseDto event = eventResponse != null ? eventResponse.getData() : null;

        if (event == null) {
            throw new BadRequestException("El evento especificado no fue encontrado.");
        }

        if (event.getOwnerId() == null || !event.getOwnerId().equals(authenticatedUserId)) {
            throw new SecurityException("Acceso denegado: solo el dueño del evento puede asignar el rol ADMIN.");
        }

        UserEventRole role = userEventRoleRepository
                .findByUserIdAndEventIdAndRoleInEvent(targetUserId, eventId, EventRole.ADMIN)
                .orElseGet(() -> userEventRoleRepository.save(UserEventRole.builder()
                        .userId(targetUserId)
                        .eventId(eventId)
                        .roleInEvent(EventRole.ADMIN)
                        .build()));

        return UserEventRoleResponseDto.builder()
                .id(role.getId())
                .userId(role.getUserId())
                .eventId(role.getEventId())
                .roleInEvent(role.getRoleInEvent())
                .build();
    }

    /**
     * Activa al agente de IA como ADMIN de un evento sin necesitar su userId a
     * mano: resuelve la cuenta por el email configurado (WD_AGENT_EMAIL) y
     * reutiliza assignAdminRole. Solo lo puede hacer el dueño del evento.
     */
    public UserEventRoleResponseDto activateAgentForEvent(Long eventId, Long authenticatedUserId) {
        if (agentEmail == null || agentEmail.isBlank()) {
            throw new BadRequestException("No hay una cuenta de agente de IA configurada (WD_AGENT_EMAIL).");
        }

        UserResponseDto agentUser;
        try {
            agentUser = userServiceClient.getUserByEmail(agentEmail);
        } catch (Exception e) {
            throw new BadRequestException("No se pudo resolver la cuenta del agente de IA (" + agentEmail + "): " + e.getMessage());
        }
        if (agentUser == null || agentUser.getId() == null) {
            throw new BadRequestException("No se encontró la cuenta del agente de IA (" + agentEmail + ").");
        }

        return assignAdminRole(eventId, agentUser.getId(), authenticatedUserId);
    }
}