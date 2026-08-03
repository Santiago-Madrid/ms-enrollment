package com.wd.ms_enrollment.service;

import com.wd.ms_enrollment.repository.EnrollmentRepository;
import com.wd.ms_enrollment.repository.UserEventRoleRepository;
import com.world_dance.wd_lib_common.dto.EnrollmentRequestDto;
import com.world_dance.wd_lib_common.dto.EnrollmentResponseDto;
import com.world_dance.wd_lib_common.entity.Enrollment;
import com.world_dance.wd_lib_common.entity.UserEventRole;
import com.world_dance.wd_lib_common.enums.EnrollmentStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserEventRoleRepository userEventRoleRepository;

    public EnrollmentResponseDto registerUserToEvent(EnrollmentRequestDto request) {

        // 1. Validar que el usuario no esté inscrito previamente en esta modalidad del evento
        boolean isAlreadyEnrolled = enrollmentRepository.existsByUserIdAndEventIdAndModalityId(
                request.getUserId(),
                request.getEventId(),
                request.getModalityId()
        );

        if (isAlreadyEnrolled) {
            throw new IllegalStateException("El usuario ya se encuentra inscrito en esta modalidad para el evento especificado.");
        }

        // 2. Asignar el rol al usuario dentro del evento si aún no lo tiene registrado
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

        // 3. Crear el registro principal de la inscripción
        Enrollment newEnrollment = Enrollment.builder()
                .userId(request.getUserId())
                .eventId(request.getEventId())
                .modalityId(request.getModalityId())
                .status(EnrollmentStatus.PENDING)
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(newEnrollment);

        // 4. Mapear y retornar la respuesta final
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














/* 
    public EnrollmentService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    // RF-25 y RF-26: Inscribirse en categoría
    @Transactional
    public EnrollmentResponseDto createEnrollment(Long userId, Long eventId, Long modalityId) {
        if (enrollmentRepository.existsByUserIdAndEventIdAndModalityId(userId, eventId, modalityId)) {
            throw new IllegalStateException("Ya te encuentras inscrito en esta categoría para este evento.");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(userId);
        enrollment.setEventId(eventId);
        enrollment.setModalityId(modalityId);
        enrollment.setStatus(EnrollmentStatus.PENDING);

        return convertToDto(enrollmentRepository.save(enrollment));
    }

    // RF-27: Aprobar inscripción manual
    @Transactional
    public EnrollmentResponseDto approveEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada con id: " + enrollmentId));

        enrollment.setStatus(EnrollmentStatus.APPROVED);
        enrollment.setRejectionReason(null); // Limpiamos cualquier rechazo previo si existía
        return convertToDto(enrollmentRepository.save(enrollment));
    }

    // RF-28: Rechazar inscripción con justificación
    @Transactional
    public EnrollmentResponseDto rejectEnrollment(Long enrollmentId, RejectionRequest rejectionRequest) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada con id: " + enrollmentId));

        enrollment.setStatus(EnrollmentStatus.REJECTED);
        enrollment.setRejectionReason(rejectionRequest.getReason());
        return convertToDto(enrollmentRepository.save(enrollment));
    }

    // RF-29: Consultar inscritos por categoría (Para Organizadores)
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDto> getEnrollmentsByCategory(Long modalityId) {
        return enrollmentRepository.findByModalityId(modalityId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // RF-30: Consultar inscripciones del participante autenticado
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDto> getMyEnrollments(Long userId) {
        return enrollmentRepository.findByUserId(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Método helper privado para transformar la entidad interna 
     * al DTO común que espera tu controlador.
     
    private EnrollmentResponseDto convertToDto(Enrollment enrollment) {
        EnrollmentResponseDto dto = new EnrollmentResponseDto();
        dto.setId(enrollment.getId());
        dto.setUserId(enrollment.getUserId());
        dto.setEventId(enrollment.getEventId());
        
        // Mapeamos de forma explícita la variable modalityId al categoryId del DTO
        dto.setCategoryId(enrollment.getModalityId());        
        // Convertimos el Enum a String si el DTO lo maneja como String text
        dto.setStatus(enrollment.getStatus() != null ? enrollment.getStatus().name() : null);
        dto.setRejectionReason(enrollment.getRejectionReason());
        
        return dto;
    }
*/
}